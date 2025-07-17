package com.autogentmcp.registry;

import org.junit.jupiter.api.Test;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.HttpEntity;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.HttpResponse;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Locale;

public class RegistryClientTest {
    
    // Custom mock implementation of CloseableHttpResponse that works in Java 8
    static class MockHttpResponse implements CloseableHttpResponse {
        private HttpEntity entity;
        private StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");

        public MockHttpResponse(HttpEntity entity) {
            this.entity = entity;
        }

        @Override
        public StatusLine getStatusLine() {
            return statusLine;
        }

        @Override
        public void setStatusLine(StatusLine statusLine) {
            this.statusLine = statusLine;
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code) {
            this.statusLine = new BasicStatusLine(ver, code, "");
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code, String reason) {
            this.statusLine = new BasicStatusLine(ver, code, reason);
        }

        @Override
        public void setStatusCode(int code) throws IllegalStateException {
            this.statusLine = new BasicStatusLine(this.statusLine.getProtocolVersion(), code, "");
        }

        @Override
        public void setReasonPhrase(String reason) throws IllegalStateException {
            this.statusLine = new BasicStatusLine(this.statusLine.getProtocolVersion(), 
                this.statusLine.getStatusCode(), reason);
        }

        @Override
        public HttpEntity getEntity() {
            return entity;
        }

        @Override
        public void setEntity(HttpEntity entity) {
            this.entity = entity;
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public void setLocale(Locale loc) {
            // No-op for test
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return this.statusLine.getProtocolVersion();
        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public Header[] getHeaders(String name) {
            return new Header[0];
        }

        @Override
        public Header getFirstHeader(String name) {
            return null;
        }

        @Override
        public Header getLastHeader(String name) {
            return null;
        }

        @Override
        public Header[] getAllHeaders() {
            return new Header[0];
        }

        @Override
        public void addHeader(Header header) {
            // No-op for test
        }

        @Override
        public void addHeader(String name, String value) {
            // No-op for test
        }

        @Override
        public void setHeader(Header header) {
            // No-op for test
        }

        @Override
        public void setHeader(String name, String value) {
            // No-op for test
        }

        @Override
        public void setHeaders(Header[] headers) {
            // No-op for test
        }

        @Override
        public void removeHeader(Header header) {
            // No-op for test
        }

        @Override
        public void removeHeaders(String name) {
            // No-op for test
        }

        @Override
        public HeaderIterator headerIterator() {
            return null;
        }

        @Override
        public HeaderIterator headerIterator(String name) {
            return null;
        }

        @Override
        @Deprecated
        public HttpParams getParams() {
            return null;
        }

        @Override
        @Deprecated
        public void setParams(HttpParams params) {
            // No-op for test
        }

        @Override
        public void close() throws IOException {
            // No-op for test
        }
    }
    
    // Custom mock implementation of HttpEntity that works in Java 8
    static class MockHttpEntity implements HttpEntity {
        private final String content;

        public MockHttpEntity(String content) {
            this.content = content;
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public boolean isChunked() {
            return false;
        }

        @Override
        public long getContentLength() {
            return content.length();
        }

        @Override
        public Header getContentType() {
            return null;
        }

        @Override
        public Header getContentEncoding() {
            return null;
        }

        @Override
        public java.io.InputStream getContent() throws IOException {
            return new ByteArrayInputStream(content.getBytes("UTF-8"));
        }

        @Override
        public void writeTo(java.io.OutputStream outStream) throws IOException {
            outStream.write(content.getBytes("UTF-8"));
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        @Deprecated
        public void consumeContent() throws IOException {
            // No-op for test
        }
    }

    @Test
    public void testRegisterEndpointsBatch() throws Exception {
        MockHttpEntity mockEntity = new MockHttpEntity("batch success");
        MockHttpResponse mockResponse = new MockHttpResponse(mockEntity);

        RegistryClient client = new RegistryClient("http://localhost:8000", "api-key") {
            @Override
            protected CloseableHttpResponse executePost(HttpPost post) throws java.io.IOException {
                return mockResponse;
            }
        };

        List<Map<String, Object>> endpoints = new ArrayList<>();
        Map<String, Object> ep = new HashMap<>();
        ep.put("name", "Test");
        ep.put("path", "/test");
        ep.put("method", "GET");
        endpoints.add(ep);
        String result = client.registerEndpointsBatch("app-key", "dev", endpoints);
        assertEquals("batch success", result);
    }

    @Test
    public void testUpdateApplication() throws Exception {
        MockHttpEntity mockEntity = new MockHttpEntity("update success");
        MockHttpResponse mockResponse = new MockHttpResponse(mockEntity);

        RegistryClient client = new RegistryClient("http://localhost:8000", "api-key") {
            @Override
            protected CloseableHttpResponse executePut(HttpPut put) throws java.io.IOException {
                return mockResponse;
            }
        };

        Map<String, Object> appData = new HashMap<>();
        appData.put("name", "App");
        appData.put("description", "Desc");
        appData.put("healthCheckUrl", "/health");
        String result = client.updateApplication("app-key", appData);
        assertEquals("update success", result);
    }
}
