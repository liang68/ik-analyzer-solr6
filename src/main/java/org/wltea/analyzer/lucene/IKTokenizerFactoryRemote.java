/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.wltea.analyzer.dic.Dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author liangyongxing
 * @editTime 2017-02-22
 * 用于远程调用更新词库
 */
public class IKTokenizerFactoryRemote extends TokenizerFactory {
  private boolean useSmart = false;

  public IKTokenizerFactoryRemote(Map<String, String> args) {
    super(args);
    this.useSmart = getBoolean(args, "useSmart", false);
  }

    @Override
    public Tokenizer create(AttributeFactory attributeFactory) {
        return new IKTokenizer(attributeFactory, useSmart());
    }

  private boolean useSmart() {
    return this.useSmart;
  }
}