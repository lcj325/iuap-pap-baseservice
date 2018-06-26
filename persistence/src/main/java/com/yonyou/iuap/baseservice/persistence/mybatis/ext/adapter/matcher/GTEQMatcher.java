package com.yonyou.iuap.baseservice.persistence.mybatis.ext.adapter.matcher;

import java.lang.reflect.Field;
import com.yonyou.iuap.baseservice.persistence.mybatis.ext.support.Match;
import com.yonyou.iuap.baseservice.persistence.mybatis.ext.support.ParamUtil;
import com.yonyou.iuap.baseservice.persistence.mybatis.ext.utils.FieldUtil;

public class GTEQMatcher implements Matcher{

	@Override
	public Match getMatch() {
		return Match.GTEQ;
	}

	@Override
	public String buildCondition(Field field, String prefix) {
		StringBuilder strb = new StringBuilder("\r\n\t<if test=\"");
		String fieldName = ParamUtil.contactParam(prefix, field.getName());
		strb.append(fieldName).append("!=null \">\r\n");
		strb.append("\t\t and ").append(FieldUtil.getColumnName(field)).append(" >= #{")
			.append(fieldName).append("}\r\n");
		strb.append("\t</if>\r\n");
		return strb.toString();
	}

}