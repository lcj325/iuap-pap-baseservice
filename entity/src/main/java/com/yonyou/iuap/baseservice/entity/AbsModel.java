package com.yonyou.iuap.baseservice.entity;

import javax.persistence.Id;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Version;

import com.yonyou.iuap.baseservice.support.condition.Condition;
import com.yonyou.iuap.baseservice.support.generator.GeneratedValue;

/**
 * 说明：基础Model-带版本号、乐观锁：ts
 * @author houlf
 * 2018年6月12日
 */
public abstract class AbsModel implements Model{

	@Id
	@GeneratedValue()
	@Column(name="id")
	@Condition
	protected Serializable id;

	@Column(name="create_time")
	protected String createTime;
	
	@Column(name="create_user")
	@Condition
	protected String createUser;
	
	@Column(name="last_modified")
	protected String lastModified;
	
	@Column(name="last_modify_user")
	@Condition
	protected String lastModifyUser;
	
	@Version
	@Column(name="ts")
	@Condition
	protected String ts;

	public Serializable getId() {
		return id;
	}
	public void setId(Serializable id) {
		this.id = id;
	}

	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getCreateUser() {
		return createUser;
	}
	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public String getLastModified() {
		return lastModified;
	}
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getLastModifyUser() {
		return lastModifyUser;
	}
	public void setLastModifyUser(String lastModifyUser) {
		this.lastModifyUser = lastModifyUser;
	}

	public String getTs() {
		return ts;
	}
	public void setTs(String ts) {
		this.ts = ts;
	}

}