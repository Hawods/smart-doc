package com.power.doc.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.power.common.util.StringUtil;
import com.power.doc.builder.ProjectDocConfigBuilder;
import com.power.doc.constants.DocTags;
import com.power.doc.helper.ParamsBuildHelper;
import com.power.doc.model.ApiParam;
import com.power.doc.model.ApiReturn;
import com.power.doc.model.DocJavaMethod;
import com.power.doc.utils.DocClassUtil;
import com.power.doc.utils.JavaClassValidateUtil;
import com.power.doc.utils.OpenApiSchemaUtil;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaType;

import static com.power.doc.constants.DocGlobalConstants.NO_COMMENTS_FOUND;
import static com.power.doc.constants.DocTags.IGNORE_RESPONSE_BODY_ADVICE;

/**
 * @author yu3.sun on 2022/10/2
 */
public interface BaseDocBuildTemplate {
  default String paramCommentResolve(String comment) {
    if (StringUtil.isEmpty(comment)) {
      comment = NO_COMMENTS_FOUND;
    } else {
      if (comment.contains("|")) {
        comment = comment.substring(0, comment.indexOf("|"));
      }
    }
    return comment;
  }

default List<ApiParam> buildReturnApiParams(
      DocJavaMethod docJavaMethod,
      ProjectDocConfigBuilder projectBuilder) {
    JavaMethod method = docJavaMethod.getJavaMethod();
    if (method.getReturns().isVoid() && Objects.isNull(projectBuilder.getApiConfig().getResponseBodyAdvice())) {
      return new ArrayList<>(0);
    }
    DocletTag downloadTag = method.getTagByName(DocTags.DOWNLOAD);
    if (Objects.nonNull(downloadTag)) {
      return new ArrayList<>(0);
    }
    String returnTypeGenericCanonicalName = method.getReturnType().getGenericCanonicalName();
    if (Objects.nonNull(projectBuilder.getApiConfig().getResponseBodyAdvice())
        && Objects.isNull(method.getTagByName(IGNORE_RESPONSE_BODY_ADVICE))) {
      String responseBodyAdvice = projectBuilder.getApiConfig().getResponseBodyAdvice().getClassName();
      if (!returnTypeGenericCanonicalName.startsWith(responseBodyAdvice)) {
        returnTypeGenericCanonicalName = new StringBuffer()
            .append(responseBodyAdvice)
            .append("<")
            .append(returnTypeGenericCanonicalName).append(">").toString();
      }
    }
    Map<String, JavaType> actualTypesMap = docJavaMethod.getActualTypesMap();
    ApiReturn apiReturn = DocClassUtil.processReturnType(returnTypeGenericCanonicalName);
    String returnType = apiReturn.getGenericCanonicalName();
    if (Objects.nonNull(actualTypesMap)) {
      for (Map.Entry<String, JavaType> entry : actualTypesMap.entrySet()) {
        returnType = returnType.replace(entry.getKey(), entry.getValue().getCanonicalName());
      }
    }

    String typeName = apiReturn.getSimpleName();
    if (this.ignoreReturnObject(typeName, projectBuilder.getApiConfig().getIgnoreRequestParams())) {
      return new ArrayList<>(0);
    }
    if (JavaClassValidateUtil.isPrimitive(typeName)) {
      docJavaMethod.setReturnSchema(OpenApiSchemaUtil.primaryTypeSchema(typeName));
      return new ArrayList<>(0);
    }
    if (JavaClassValidateUtil.isCollection(typeName)) {
      if (returnType.contains("<")) {
        String gicName = returnType.substring(returnType.indexOf("<") + 1, returnType.lastIndexOf(">"));
        if (JavaClassValidateUtil.isPrimitive(gicName)) {
          docJavaMethod.setReturnSchema(OpenApiSchemaUtil.arrayTypeSchema(gicName));
          return new ArrayList<>(0);
        }
        return ParamsBuildHelper.buildParams(gicName, "", 0, null, Boolean.TRUE,
            new HashMap<>(), projectBuilder, null, 0, Boolean.FALSE, null);
      } else {
        return new ArrayList<>(0);
      }
    }
    if (JavaClassValidateUtil.isMap(typeName)) {
      String[] keyValue = DocClassUtil.getMapKeyValueType(returnType);
      if (keyValue.length == 0) {
        return new ArrayList<>(0);
      }
      return ParamsBuildHelper.buildParams(returnType, "", 0, null, Boolean.TRUE,
          new HashMap<>(), projectBuilder, null, 0, Boolean.FALSE, null);
    }
    if (StringUtil.isNotEmpty(returnType)) {
      return ParamsBuildHelper.buildParams(returnType, "", 0, null, Boolean.TRUE,
          new HashMap<>(), projectBuilder, null, 0, Boolean.FALSE, null);
    }
    return new ArrayList<>(0);
  }

  boolean ignoreReturnObject(String typeName, List<String> ignoreParams);
}
