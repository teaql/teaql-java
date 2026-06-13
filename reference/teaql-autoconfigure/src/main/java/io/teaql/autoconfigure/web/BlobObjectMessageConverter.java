package io.teaql.autoconfigure.web;

import io.teaql.core.web.BlobObject;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.ClassUtil;

public class BlobObjectMessageConverter extends AbstractHttpMessageConverter<BlobObject> {

    public BlobObjectMessageConverter() {
        super(MediaType.ALL);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ClassUtil.isAssignable(BlobObject.class, clazz);
    }

    @Override
    protected Long getContentLength(BlobObject blobObject, MediaType contentType) throws IOException {
        return (long) ArrayUtil.length(blobObject.getData());
    }

    @Override
    protected BlobObject readInternal(
            Class<? extends BlobObject> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        long length = inputMessage.getHeaders().getContentLength();
        byte[] bytes =
                length >= 0 && length < Integer.MAX_VALUE
                        ? inputMessage.getBody().readNBytes((int) length)
                        : inputMessage.getBody().readAllBytes();
        return BlobObject.binaryStream(
                bytes, inputMessage.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    protected void writeInternal(BlobObject blobObject, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (blobObject == null) {
            return;
        }
        StreamUtils.copy(blobObject.getData(), outputMessage.getBody());
    }

    @Override
    protected void addDefaultHeaders(
            HttpHeaders headers, BlobObject blobObject, MediaType contentType) throws IOException {
        Map<String, String> objectHeaders = blobObject.getHeaders();
        if (objectHeaders != null) {
            for (Map.Entry<String, String> entry : objectHeaders.entrySet()) {
                headers.add(entry.getKey(), entry.getValue());
            }
        }
        super.addDefaultHeaders(headers, blobObject, contentType);
    }
}
