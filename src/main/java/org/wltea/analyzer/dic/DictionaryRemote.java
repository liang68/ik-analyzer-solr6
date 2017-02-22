/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
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
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 * 
 */
package org.wltea.analyzer.dic;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.wltea.analyzer.cfg.ConfigurationRemote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 词典管理类,单例模式
 * add by liangyongxing
 * @createTime 2017-02-22
 * 用于远程调用更新词库
 */
public class DictionaryRemote {
	/*
	 * 词典单子实例
	 */
	private static DictionaryRemote singleton;

	/*
	 * 主词典对象
	 */
	private DictSegment _MainDict;

	/*
	 * 停止词词典
	 */
	private DictSegment _StopWordDict;
	/*
	 * 量词词典
	 */
	private DictSegment _QuantifierDict;

	/**
	 * 配置对象
	 */
	private ConfigurationRemote cfg;

	/**
	 * 创建包含有一个线程的线程池
	 */
	private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

	private DictionaryRemote(ConfigurationRemote cfg){
		this.cfg = cfg;
		this.loadMainDict();
		this.loadStopWordDict();
		this.loadQuantifierDict();
	}
	
	/**
	 * 词典初始化
	 * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
	 * 只有当Dictionary类被实际调用时，才会开始载入词典，
	 * 这将延长首次分词操作的时间
	 * 该方法提供了一个在应用加载阶段就初始化字典的手段
	 * @return Dictionary
	 */
	public static DictionaryRemote initial(ConfigurationRemote cfg){
		if(singleton == null){
			synchronized(DictionaryRemote.class){
				if(singleton == null){
					singleton = new DictionaryRemote(cfg);
					return singleton;
				}
			}
			/**
			 * 定时任务执行
			 */
			for (String location : cfg.getRemoteExtDictionarys()) {
				pool.scheduleAtFixedRate(new Monitor(location), 10L, 60L, TimeUnit.SECONDS);
			}
			for (String location : cfg.getRemoteExtStopWordDictionarys()) {
				pool.scheduleAtFixedRate(new Monitor(location), 10L, 60L, TimeUnit.SECONDS);
			}
		}
		return singleton;
	}
	
	/**
	 * 获取词典单子实例
	 * @return Dictionary 单例对象
	 */
	public static DictionaryRemote getSingleton(){
		if(singleton == null){
			throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
		}
		return singleton;
	}

	/**
	 * 批量加载新词条
	 * @param words Collection<String>词条列表
	 */
	public void addWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量加载词条到主内存词典中
					singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 批量移除（屏蔽）词条
	 * @param words
	 */
	public void disableWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量屏蔽词条
					singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray){
		return singleton._MainDict.match(charArray);
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray , int begin, int length){
		return singleton._MainDict.match(charArray, begin, length);
	}
	
	/**
	 * 检索匹配量词词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray , int begin, int length){
		return singleton._QuantifierDict.match(charArray, begin, length);
	}
	
	
	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 * @param charArray
	 * @param currentIndex
	 * @param matchedHit
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray , int currentIndex , Hit matchedHit){
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1 , matchedHit);
	}
	
	
	/**
	 * 判断是否是停止词
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray , int begin, int length){			
		return singleton._StopWordDict.match(charArray, begin, length).isMatch();
	}	
	
	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict(){
		//建立一个主词典实例
		_MainDict = new DictSegment((char)0);
		//读取主词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getMainDictionary());
        if(is == null){
        	throw new RuntimeException("Main Dictionary not found!!!");
        }
        
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);
			
		} catch (IOException ioe) {
			System.err.println("Main Dictionary loading exception.");
			ioe.printStackTrace();
			
		}finally{
			streamClose(is);
		}
		//加载扩展词典
		this.loadExtDict();

		//加载远程扩展词典
		loadRemoteExtDict();
	}	
	
	/**
	 * 加载用户配置的扩展词典到主词库表
	 */
	private void loadExtDict(){
		//加载扩展词典配置
		List<String> extDictFiles  = cfg.getExtDictionarys();
		if(extDictFiles != null){
			InputStream is = null;
			for(String extDictName : extDictFiles){
				//读取扩展词典文件
				System.out.println("加载扩展词典：" + extDictName);
				is = this.getClass().getClassLoader().getResourceAsStream(extDictName);
				//如果找不到扩展的字典，则忽略
				if(is == null){
					continue;
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
					String theWord = null;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {
							//加载扩展词典数据到主内存词典中
							//System.out.println(theWord);
							_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
						}
					} while (theWord != null);
					
				} catch (IOException ioe) {
					System.err.println("Extension Dictionary loading exception.");
					ioe.printStackTrace();
					
				}finally{
					streamClose(is);
				}
			}
		}		
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict(){
		//建立一个主词典实例
		_StopWordDict = new DictSegment((char)0);
		//加载扩展停止词典
		List<String> extStopWordDictFiles  = cfg.getExtStopWordDictionarys();
		if(extStopWordDictFiles != null){
			InputStream is = null;
			for(String extStopWordDictName : extStopWordDictFiles){
				System.out.println("加载扩展停止词典：" + extStopWordDictName);
				//读取扩展词典文件
				is = this.getClass().getClassLoader().getResourceAsStream(extStopWordDictName);
				//如果找不到扩展的字典，则忽略
				if(is == null){
					continue;
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
					String theWord = null;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {
							//System.out.println(theWord);
							//加载扩展停止词典数据到内存中
							_StopWordDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
						}
					} while (theWord != null);
					
				} catch (IOException ioe) {
					System.err.println("Extension Stop word Dictionary loading exception.");
					ioe.printStackTrace();
					
				}finally{
					streamClose(is);
				}
			}
		}
		/**
		 * 加载远程外部停用词表
		 */
		List<String> remoteExtStopWordDictFiles = this.cfg.getRemoteExtStopWordDictionarys();
		for (String location : remoteExtStopWordDictFiles) {
			System.out.println("[remote dict Loading]" + location);
			List<String> lists = getRemoteWords(location);

			if (lists == null) {
				System.err.println("[Dict Loading]" + location + "加载失败");
			}
			else {
				for (String theWord : lists) {
					if ((theWord != null) && (!"".equals(theWord.trim()))) {
						this._StopWordDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
					}
				}
			}
		}
	}

	/**
	 * 加载远程扩展词库
	 */
	private void loadRemoteExtDict()
	{
		List<String> remoteExtDictFiles = this.cfg.getRemoteExtDictionarys();
		for (String location : remoteExtDictFiles) {
			List<String> lists = getRemoteWords(location);

			if (lists == null) {
				System.err.println("[remote dict Loading]" + location + "加载失败");
			}
			else
				for (String theWord : lists)
					if ((theWord != null) && (!"".equals(theWord.trim())))
					{
						this._MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
					}
		}
	}

	/**
	 * 实际加载远程文件词到内存
	 * @param location
	 * @return
     */
	private static List<String> getRemoteWords(String location)
	{
		List<String> buffer = new ArrayList();
		RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10000).setConnectTimeout(10000).setSocketTimeout(60000).build();

		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet get = new HttpGet(location);
		get.setConfig(rc);
		try {
			CloseableHttpResponse response = httpclient.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				String charset = "UTF-8";

				if (response.getEntity().getContentType().getValue().contains("charset=")) {
					String contentType = response.getEntity().getContentType().getValue();
					charset = contentType.substring(contentType.lastIndexOf("=") + 1);
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
				String line;
				while ((line = in.readLine()) != null) {
					buffer.add(line);
				}
				in.close();
				response.close();
				return buffer;
			}
			response.close();
		} catch (ClientProtocolException e) {
			System.err.println("getRemoteWords ClientProtocolException error" + e.getStackTrace());
		} catch (IllegalStateException e) {
			System.err.println("getRemoteWords IllegalStateException error" + e.getStackTrace());
		} catch (IOException e) {
			System.err.println("getRemoteWords IOException error" + e.getStackTrace());
		}
		return buffer;
	}
	
	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict(){
		//建立一个量词典实例
		_QuantifierDict = new DictSegment((char)0);
		//读取量词词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getQuantifierDicionary());
        if(is == null){
        	throw new RuntimeException("Quantifier Dictionary not found!!!");
        }
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_QuantifierDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);
			
		} catch (IOException ioe) {
			System.err.println("Quantifier Dictionary loading exception.");
			ioe.printStackTrace();
			
		}finally{
			streamClose(is);
		}
	}

	/**
	 * 将公共关闭封装,减少重复代码
	 * add
	 * @author liangyongxing
	 * @param is
	 * @throws IOException
	 * @createTime 2017/02/07
     */
	private void streamClose(InputStream is) {
		try {
			if(is != null){
				is.close();
				is = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @Description 清空字典缓存，用于动态更新字典表
	 * @author liangyongxing
	 * @createTime 2017/02/07
	 */
	public static void clear(){
		singleton = null;
	}

	public void reLoadMainDict() {
		DictionaryRemote tmpDict = new DictionaryRemote(getSingleton().cfg);
		tmpDict.loadMainDict();
		tmpDict.loadStopWordDict();
		this._MainDict = tmpDict._MainDict;
		this._StopWordDict = tmpDict._StopWordDict;
		System.out.println("重新加载词典完毕...");
	}
}
