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

import org.junit.Test;
import org.openqa.selenium.remote.http.HttpResponse;

public class HtmlUnitDriverRemoteSessionTest extends RemoteWebDriverTestCase {
    @Test
    public void shouldBeAbleToCreateNewSession() {
        assertNotNull("Session ID", sessionId);
    }
    
    @Test
    public void shouldBeAbleToDeleteSession() {
        HttpResponse response = HtmlUnitDriverServer.deleteSession(sessionId);
        assertEquals("Failed deleting session", HTTP_OK, response.getStatus());
    }
}