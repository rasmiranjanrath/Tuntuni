/*
 * Copyright 2016 Tuntuni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tuntuni.video;

import java.util.LinkedList;

/**
 * A push and pop based data line for audio or image frame.
 *
 * @param <T> Type of object to pass in this line
 */
public class DataLine<T extends DataFrame> {

    private long mStart;
    private final LinkedList<T> mData;

    public DataLine() {
        mStart = 0;
        mData = new LinkedList<>();
    }

    /**
     * Gets the start time of first push to this data line.
     *
     * @return
     */
    public long getStart() {
        return mStart;
    }

    /**
     * Gets the data list.
     *
     * @return
     */
    public LinkedList<T> getData() {
        return mData;
    }

    /**
     * Block until at least one input has received.
     *
     * @return
     */
    public T pop() {
        int test = 0;
        while (getData().isEmpty()) {
            // block until mData is free
            test++;
        }
        System.out.println("POP_TEST = " + test);
        return mData.pollFirst();
    }

    /**
     * Add a data to the list
     *
     * @param data
     */
    public void push(T data) {
        if (mStart == 0) {
            mStart = System.nanoTime();
        }
        getData().addLast(data);
    }
}