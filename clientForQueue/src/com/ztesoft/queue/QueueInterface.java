package com.ztesoft.queue;



public interface QueueInterface {
	
	/**
	 * ����put����������Ϣ���������
	 * 
	 * @param queueName
	 * @param key
	 * @param value
	 * @
	 */
	public Boolean put(String queueName,String key,  Object value) ;


    /**
     * ����pop����������һ����Ϣ
     * @param queueName
     * @
     */
    public Object pop(String queueName) ;

	/**
	 * ���г��Ȳ���
	 * 
	 * @param queueName
	 * @param key
	 * @return
	 * @
	 */
	public long count(String queueName);

}