package com.hwhub.batch.infrastructure.mybatis.converter;

import com.hwhub.batch.domain.enums.LocaleType;
import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel;
import com.hwhub.batch.domain.model.inquiry.InquiryModel;
import com.hwhub.batch.infrastructure.mybatis.custom.entity.InquiryWithMessagesEntity;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.TInquiryMessage;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class InquiryConverter {

  public static InquiryModel toModel(InquiryWithMessagesEntity entity) {
    List<InquiryMessageModel> messages =
        entity.getMessages() == null
            ? Collections.emptyList()
            : entity.getMessages().stream().map(InquiryConverter::toMessageModel).toList();
    String localeCode = entity.getUserLocale();
    LocaleType locale;
    try {
      locale = LocaleType.fromCode(localeCode != null ? localeCode : "ja");
    } catch (IllegalArgumentException e) {
      locale = LocaleType.JA;
    }
    return InquiryModel.reconstruct(
        entity.getInquiryId(),
        entity.getUserId(),
        entity.getTitle(),
        entity.getStatus(),
        messages,
        locale);
  }

  public static InquiryMessageModel toMessageModel(TInquiryMessage entity) {
    return InquiryMessageModel.reconstruct(
        entity.getMessageId(),
        entity.getInquiryId(),
        entity.getSeq(),
        entity.getSenderType(),
        entity.getBody());
  }
}
