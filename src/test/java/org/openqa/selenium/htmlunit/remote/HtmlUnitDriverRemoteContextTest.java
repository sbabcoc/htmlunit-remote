// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.htmlunit.remote;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

public class HtmlUnitDriverRemoteContextTest extends RemoteWebDriverTestCase {
    @Test
    public void shouldBeAbleToGetWindowHandle() {
        HttpResponse response = server.getWindowHandle(sessionId);
        assertEquals("Failed getting window handle", HTTP_OK, response.getStatus());
        String windowHandle = extractString(response);
        assertTrue("Window handle should be integer string; was " + windowHandle, windowHandle.matches("\\d+"));
    }
    
    @Test(expected = NoSuchSessionException.class)
    public void shouldBeAbleToOpenAndCloseWindows() throws Exception {
        String handleOne = getWebDriver().getWindowHandle();
        CommandPayload payload = DriverCommand.SWITCH_TO_NEW_WINDOW(WindowType.WINDOW);
        HttpRequest request = commandCodec.encode(new Command(sessionId(), payload));
        HttpResponse response = server.switchToNewWindow(request, sessionId);
        assertEquals("Failed switching to new window", HTTP_OK, response.getStatus());
        Map<String, Object> newWindow = extractMap(response);
        assertEquals("Type of new window", "window", newWindow.get("type"));
        assertTrue("Handle of new window is not a String", newWindow.get("handle") instanceof String);
        String handleTwo = (String) newWindow.get("handle");
        response = server.getWindowHandles(sessionId);
        assertEquals("Failed getting window handles", HTTP_OK, response.getStatus());
        List<String> windowHandles = extractListOfStrings(response);
        assertEquals("Window handles after creation", Set.of(handleOne, handleTwo), new HashSet<String>(windowHandles));
        payload = DriverCommand.SWITCH_TO_WINDOW(handleOne);
        request = commandCodec.encode(new Command(sessionId(), payload));
        response = server.switchToWindow(request, sessionId);
        assertEquals("Failed switching to initial window", HTTP_OK, response.getStatus());
        assertEquals("Window handle after switching", handleOne, getWebDriver().getWindowHandle());
        response = server.closeWindow(sessionId);
        assertEquals("Failed closing initial window", HTTP_OK, response.getStatus());
        windowHandles = extractListOfStrings(response);
        assertEquals("Window handles after closing initial window", List.of(handleTwo), windowHandles);
        response = server.closeWindow(sessionId);
        assertEquals("Failed closing second window", HTTP_OK, response.getStatus());
        windowHandles = extractListOfStrings(response);
        assertEquals("Window handles after closing second window", List.of(), windowHandles);
        getWebDriver();
        fail("Driver session still exists after closing all windows");
    }
    
    @Test
    public void shouldBeAbleToSwitchToFrame() throws Exception {
        final String page1 = HTMLUNIT_HOME;
        final String page2 = SELENIUM_DEV;
        final String frame1 = "<iframe id='id1' name='name1' title='HtmlUnit Home' src='" + page1 + "'></iframe>";
        final String frame2 = "<iframe id='id2' name='name2' title='Selenium Dev' src='" + page2 + "'></iframe>";
        
        final By main_frame1 = By.cssSelector("iframe#id1");
        final By main_frame2 = By.cssSelector("iframe#id2");
        final By frame1_elem = By.cssSelector("img[alt=HtmlUnit]");
        final By frame2_elem = By.cssSelector("a.navbar-brand");
        
        loadPage2("<html><head><title>frames example</title></head><body><h1 id='header'>Frames Example</h1>" +
                frame1 + frame2 + "</body></html>");
        
        // [FRAME BY INDEX]
        CommandPayload payload = DriverCommand.SWITCH_TO_FRAME(0);
        HttpRequest request = commandCodec.encode(new Command(sessionId(), payload));
        HttpResponse response = server.switchToFrame(request, sessionId);
        assertEquals("Failed switching to frame by index", HTTP_OK, response.getStatus());
        verifyContextElement(frame1_elem, page1);
        
        // [PARENT FRAME]
        response = server.switchToParentFrame(sessionId);
        assertEquals("Failed switching to parent frame", HTTP_OK, response.getStatus());
        verifyContextElement(main_frame1, "parent frame");
        
        // [FRAME BY ID]
        payload = DriverCommand.SWITCH_TO_FRAME("id1");
        request = commandCodec.encode(new Command(sessionId(), payload));
        response = server.switchToFrame(request, sessionId);
        assertEquals("Failed switching to frame by ID", HTTP_OK, response.getStatus());
        verifyContextElement(frame1_elem, page1);
        
        // [DEFAULT CONTENT] (id = null)
        payload = DriverCommand.SWITCH_TO_FRAME(null);
        request = commandCodec.encode(new Command(sessionId(), payload));
        response = server.switchToFrame(request, sessionId);
        assertEquals("Failed switching to default content", HTTP_OK, response.getStatus());
        verifyContextElement(main_frame1, "default content");
        
        // [FRAME BY NAME]
        payload = DriverCommand.SWITCH_TO_FRAME("name2");
        request = commandCodec.encode(new Command(sessionId(), payload));
        response = server.switchToFrame(request, sessionId);
        assertEquals("Failed switching to frame by name", HTTP_OK, response.getStatus());
        verifyContextElement(frame2_elem, page2);
        
        // [PARENT FRAME]
        response = server.switchToParentFrame(sessionId);
        assertEquals("Failed switching to parent frame", HTTP_OK, response.getStatus());
        verifyContextElement(main_frame1, "parent frame");
                
        // [FRAME BY ELEMENT]
        payload = DriverCommand.SWITCH_TO_FRAME(getWebDriver().findElement(main_frame2));
        request = commandCodec.encode(new Command(sessionId(), payload));
        response = server.switchToFrame(request, sessionId);
        assertEquals("Failed switching to frame by element", HTTP_OK, response.getStatus());
        verifyContextElement(frame2_elem, page2);
        
        // [DEFAULT CONTENT] (id = null)
        payload = DriverCommand.SWITCH_TO_FRAME(null);
        request = commandCodec.encode(new Command(sessionId(), payload));
        response = server.switchToFrame(request, sessionId);
        assertEquals("Failed switching to default content", HTTP_OK, response.getStatus());
        verifyContextElement(main_frame1, "default content");
    }
    
    @Test
    public void shouldBeAbleToGetWindowRect() {
        HttpResponse response = server.getWindowRect(sessionId);
        assertEquals("Failed getting window rect", HTTP_OK, response.getStatus());
        Map<String, Object> windowRect = extractMap(response);
        assertEquals("Window width", 1272L, windowRect.get("width"));
        assertEquals("Window height", 753L, windowRect.get("height"));
        assertEquals("Window 'x' position", 0L, windowRect.get("x"));
        assertEquals("Window 'y' position", 0L, windowRect.get("y"));
    }
    
    @Test
    public void shouldBeAbleToSetWindowRect() {
        CommandPayload payload = new CommandPayload(DriverCommand.SET_CURRENT_WINDOW_SIZE,
                Map.of("width", 640, "height", 480, "x", 360, "y", 240));
        HttpRequest request = commandCodec.encode(new Command(sessionId(), payload));
        HttpResponse response = server.setWindowRect(request, sessionId);
        assertEquals("Failed setting window rect", HTTP_OK, response.getStatus());
        Map<String, Object> windowRect = extractMap(response);
        assertEquals("Window width", 640L, windowRect.get("width"));
        assertEquals("Window height", 480L, windowRect.get("height"));
        assertEquals("Window 'x' position", 360L, windowRect.get("x"));
        assertEquals("Window 'y' position", 240L, windowRect.get("y"));
    }
    
    @Test
    public void shouldBeAbleToMaximizeWindow() {
        HttpResponse response = server.maximizeWindow(sessionId);
        assertEquals("Failed maximizing window", HTTP_OK, response.getStatus());
        Map<String, Object> windowRect = extractMap(response);
        assertEquals("Window width", 1272L, windowRect.get("width"));
        assertEquals("Window height", 753L, windowRect.get("height"));
        assertEquals("Window 'x' position", 0L, windowRect.get("x"));
        assertEquals("Window 'y' position", 0L, windowRect.get("y"));
    }
    
    @Test
    public void shouldBeAbleToFullscreenWindow() {
        HttpResponse response = server.fullscreenWindow(sessionId);
        assertEquals("Failed maximizing window", HTTP_OK, response.getStatus());
        Map<String, Object> windowRect = extractMap(response);
        assertEquals("Window width", 1272L, windowRect.get("width"));
        assertEquals("Window height", 753L, windowRect.get("height"));
        assertEquals("Window 'x' position", 0L, windowRect.get("x"));
        assertEquals("Window 'y' position", 0L, windowRect.get("y"));
    }
    
    private void verifyContextElement(final By locator, final String contextName) {
        try {
            getWebDriver().findElement(locator);
        } catch (NoSuchElementException e) {
            fail("Failed locating element in " + contextName);
        }
    }
}