package com.bishugui.summer.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author bi shugui
 * @description 输入流回调
 * @date 2023/9/27 21:55
 */
@FunctionalInterface
public interface InputStreamCallback<T> {
    /**
     * 处理输入流
     * @param stream
     * @return
     * @throws IOException
     */
    T doWithInputStream(InputStream stream) throws IOException;
}
