package com.hwhub.batch.domain.ai;

import com.hwhub.batch.domain.model.inquiry.InquiryModel;

public interface AiClient {
  /** 問い合わせ内容とナレッジをもとに回答を生成する。 API失敗時は例外をスローする（呼び出し側でキャッチして後続停止）。 */
  String ask(InquiryModel inquiry, String knowledge);
}
