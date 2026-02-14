package com.study.blog.core.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ExceptionMessage {

  // AuthenticationException
  JWE_VERIFY_FAILED("JWE_VERIFY_FAILED"),
  ACCESS_KEY_NOT_EXIST("존재하지 않는 접근 키 입니다."),
  ACCESS_KEY_ALREADY_EXIST("이미 존재하는 접근 키 입니다."),
  INVALID_TOKEN("잘못된 토큰 정보입니다."),
  REFRESH_TOKEN_EXPIRED("리프레시 토큰이 만료되었습니다."),
  PLEASE_CHECK_USER_INFO("사용자 정보를 확인해 주세요."),
  TOKEN_TYPE_ERROR("토큰 타입 오류가 발생하였습니다."),
  TOKEN_NOT_FOUND("토큰을 찾을 수 없습니다."),
  UNSUPPORTED_TOKEN("미지원 토큰입니다."),
  TOKEN_COUNTERFEITED("토큰이 변조되었습니다."),
  TOKEN_MALFORMED("토큰이 손상되었습니다."),
  TOKEN_ERROR("토큰 오류가 발생하였습니다."),
  TOKEN_EXPIRED("토큰이 만료되었습니다."),
  USER_DUPLICATION_ERROR("사용자 중복이 발생하였습니다."),

  // ApiRequestException
  // 기사
  INCORRECT_ARTICLE_FORMAT("잘못된 기사 형식 입니다."),
  CAN_ONLY_TRANSMIT_RELEASED_ARTICLES("출고된 기사만 전송할 수 있습니다."),
  ARTICLE_NOT_APPROVED("출고되지 않은 기사입니다."),
  ARTICLE_NOT_FOUND("존재하지 않는 기사입니다."),
  REPORTER_INFO_NOT_EXIST("기자 정보가 없습니다."),
  CHANNEL_INFO_NOT_EXIST("지역사 정보가 없습니다."),
  INVALID_SCHEDULED_TIME("예정 시간을 현재 시간 이후로 설정해주세요."),
  INVALID_SCHEDULED_DATE("예정 일자를 현재 일자 이후로 설정해주세요."),
  ARTICLE_RECORD_NOT_FOUND("기사 기록을 찾을 수 없습니다."),
  ARTICLE_ALREADY_UNLOCKED("이미 잠금이 해제된 기사 입니다."),
  UNLOCK_NOT_PERMITTED("잠금자 본인이 아니므로 잠금 해제가 불가합니다."),
  ARTICLE_ALREADY_PROVIDED("이미 제공된 기사입니다."),
  ARTICLE_PROVISION_ALREADY_CANCELED("이미 제공이 취소된 기사입니다."),
  ARTICLE_ALREADY_DISABLED("이미 사용 중지된 기사입니다."),
  DISABLED_ARTICLE_CANNOT_MATCH_CUESHEET("사용 중지된 기사는 추가하실 수 없습니다."),

  ARTICLE_COMPOSE_ALREADY_UNLOCKED("이미 잠금이 해제된 구성안 입니다."),
  ARTICLE_COMPOSE_NOT_FOUND("존재하지 않는 기사 구성안입니다."),


  ARTICLE_ALREADY_APPROVED("이미 출고된 기사입니다."),
  ARTICLE_ALREADY_IN_SAME_APPROVED_STATE("이미 동일한 출고 상태로 변경되었습니다."),
  ARTICLE_LOCKED_BY_USER("에 의해 잠긴 기사입니다."),

  INVALID_PERMISSION_TO_DELETE_ARTICLE("해당 기사를 삭제할 권한이 없습니다."),
  CANNOT_DELETED_WHILE_MODIFYING("수정 중이므로 삭제할 수 없습니다. 수정자"),
  ARTICLE_CANNOT_DELETED_IN_APPROVED_STATE("출고 상태이므로 삭제할 수 없습니다."),
  ARTICLE_CANNOT_DELETED_IN_QUESHEET_MATCHING_STATE("큐시트 매칭 상태이므로 삭제할 수 없습니다."),
  ARTICLE_CANNOT_REPLACE("대체 기사를 작성할 수 없습니다."),
  ARTICLE_CANNOT_DELETED_IN_QUESHEET("큐시트에 포함된 기사이므로 삭제할 수 없습니다."),

  UNLOCKED_ARTICLE_CANNOT_BE_SAVED("잠금이 해제되어 저장할 수 없습니다."),
  ARTICLE_CANNOT_MODIFIED_AFTER_APPROVED("출고된 기사이므로 수정이 불가합니다."),
  TRANSFER_CANCEL_NOT_PERMITTED("해당 공용이관을 반려할 권한이 없습니다."),
  APPROVED_ARTICLE_CANNOT_TRANSFER_CANCEL("데스킹 된 기사는 공용이관을 반려할 수 없습니다."),
  ALREADY_TRANSFERRED_MY_ARTICLE("이미 공용이관된 기사입니다."),
  TARGET_VIEWER_DESKER_IS_REQUIRED("데스커 지정은 필수입니다."),
  DELETE_MY_ARTICLE_NOT_PERMITTED("해당 기사 삭제 권한이 없습니다."),
  EMBARGO_ARTICLE_CANNOT_ADD_CUESHEET("엠바고 기사를 추가할 수 없습니다. 남은 시간(분)"),

  ARTICLE_CAPTION_LENGTH_OVER_LIMIT_LENGTH("제한된 자막 길이를 초과합니다."),
  CAPTION_NOT_EXIST("존재하지 않는 자막입니다."),
  DIGITAL_NOT_EXIST("존재하지 않는 디지털 정보입니다."),

  IS_ARTICLE_COMPLETED("이미 완료된 기사입니다."),
  IS_ARTICLE_TRANSFERRED("이미 출고된 기사입니다."),
  IS_TRANSFER_AFTER_COMPLETION("완료 후에 출고하십시오."),
  IS_COMPLETION_CANCELLABLE("완료된 기사에 한하여 완료취소를 하실 수 있습니다."),
  IS_TRANSFER_CANCELLABLE("출고된 기사에 한하여 출고취소를 하실 수 있습니다."),
  IS_IN_WRITING("작성중인 기사에 한하여 완료를 하실 수 있습니다."),
  NOT_DELETED_ARTICLE("삭제된 기사가 아닙니다."),

  MY_ARTICLE_NOT_FOUND("본인이 작성한 개인기사가 존재하지 않습니다."),
  // 자막 템플릿
  CAPTION_TEMPLATE_NOT_EXIST("존재하지 않는 자막 템플릿입니다."),
  CAPTION_TEMPLATE_ITEM_NOT_EXIST("존재하지 않는 자막 템플릿 아이템입니다."),
  CAPTION_TEMPLATE_ITEM_GROUP_LIST_IS_REQUIRED("자막 템플릿 아이템 그룹 목록은 필수입니다."),
  CAPTION_TEMPLATE_ITEM_MULTILINE_NOT_ALLOWED("멀티라인을 지원하지 않는 자막 템플릿 아이템입니다"),
  VIZRT_TEMPLATE_IN_USE("해당 vizrt 템플릿을 사용하는 자막 템플릿이 존재합니다."),

  // 연합 기사
  YONHAP_ARTICLE_NOT_FOUND("존재하지 않는 연합 기사입니다."),
  YONHAP_ARTICLE_NOT_FOUND_IN_ENGLISH("yonhap article not found"),
  YONHAP_ARTICLE_ALREADY_EXIST_IN_ENGLISH("yonhap article already exist"),


  //기사 플랫폼 정보
  ARTICLE_PLATFORM_NOT_EXIST("기사 플랫폼 정보가 존재하지 않습니다."),
  ARTICLE_ALREADY_EXIST_PLATFORM("해당 기사의 해당 플랫폼 정보가 이미 있습니다."),

  // 사용자
  SSO_CODE_ISSUE_FAIL("SSO 코드 발급에 실패 했습니다."),
  SSO_SESSION_INVALID("유효 하지 않은 코드"),
  INCORRECT_PASSWORD("비밀번호가 잘못 입력 되었습니다."),
  USER_INFO_NOT_EXIST("사용자 정보가 없습니다."),
  USER_NOT_EXIST("NDS 시스템에 존재하지 않는 사용자입니다. 관리자에게 문의해주세요"),
  INCORRECT_USER_ID("잘못된 사용자 ID가 있습니다."),
  ALREADY_EXISTING_ID("이미 존재하는 ID 입니다."),
  USER_CHANNEL_INFO_NOT_EXIST("사용자 채널 정보가 없습니다."),
  PLEASE_CHECK_USER_ACTIVITY("사용자 활성 여부를 확인하세요."),
  SETTINGS_INFO_NOT_EXIST("설정 정보가 존재하지 않습니다."),
  SOME_USER_INFO_NOT_EXIST("사용자의 일부 정보가 없습니다."),
  HASHING_ERROR("사용자 정보 암호화에 실패하였습니다."),
  USER_EMAIL_INFO_NOT_EXIST("사용자의 이메일 정보가 없습니다."),
  USER_LOGIN_INFO_NOT_EXIST("사용자의 접속 정보가 없습니다."),
  USER_NOT_VALID("유효하지 않은 사용자입니다."),

  //조직
  ORGANIZATION_NOT_EXIST("조직 정보가 없습니다."),
  HIERARCHY_NOT_VALID("조직 계층이 잘못되었습니다."),
  //부서

  // 큐시트
  CUE_SHEET_NOT_EXIST("존재하지 않는 큐시트입니다."),
  LOCKED_CUE_SHEET_CANNOT_BE_DELETED("잠금 상태인 큐시트는 삭제할 수 없습니다."),
  CONFIRMED_OR_BROADCASTING_CUE_SHEET_CANNOT_BE_DELETED("확정 또는 방송중인 큐시트는 삭제할 수 없습니다."),
  LOCKED_CUE_SHEET("해당 큐시트는 잠금 상태입니다."),
  CUE_SHEET_NOT_CONFIRMED("해당 큐시트는 확정 상태가 아닙니다."),
  CUE_SHEET_DVE_REQUIRED("DVE가 요청된 큐시트만 조회할 수 있습니다."),

  CUE_SHEET_TEMPLATE_ITEM_ORDER_OUT_OF_RANGE("큐시트 템플릿 아이템 목록의 순번 범위를 초과합니다. cueTmpltItemOrd : "),
  CUE_SHEET_TEMPLATE_ITEM_NOT_EXIST("존재하지 않는 큐시트 템플릿 아이템입니다."),
  CUE_SHEET_TEMPLATE_ITEM_CAP_NOT_EXIST("큐시트 템플릿 아이템 자막을 찾을 수 없습니다."),

  CUE_SHEET_ITEM_ORDER_OUT_OF_RANGE("큐시트 아이템 목록의 순번 범위를 초과합니다. cueItemOrd : "),
  CUE_SHEET_ITEM_IS_LOCKED("해당 큐시트 아이템은 잠금 상태입니다."),
  CUE_SHEET_ITEM_IS_UNLOCKED("큐시트 아이템 잠금이 해제된 상태입니다."),
  CUE_SHEET_ITEM_NOT_EXIST("존재하지 않는 큐시트 아이템입니다."),
  LOCKED_CUE_SHEET_ITEM("해당 큐시트 아이템은 잠금 상태입니다. 잠금자"),
  LOCKED_CUE_SHEET_ITEM_CANNOT_BE_DELETED("잠금 상태인 큐시트는 삭제할 수 없습니다."),
  CUE_SHEET_ITEM_ORDER_OUT_OF_AREA_SPARE("예비영역에 아이템 추가(이동)을 실패하였습니다. cueItemOrd : "),
  CUE_SHEET_ITEM_ORDER_OUT_OF_AREA_ACTIVE("활성영역에 아이템 추가(이동)을 실패하였습니다. cueItemOrd : "),

  LOCKED_CUE_SHEET_ITEM_ANC_MENT("해당 큐시트 아이템 앵커멘트는 잠금 상태입니다. 잠금자"),
  CUE_SHEET_ITEM_ANC_MENT_IS_UNLOCKED("큐시트 아이템 앵커멘트 잠금이 해제된 상태입니다."),

  LOCKED_CUE_SHEET_ITEM_TITLE("해당 큐시트 아이템 제목은 잠금 상태입니다. 잠금자"),
  CUE_SHEET_ITEM_TITLE_IS_UNLOCKED("큐시트 아이템 제목 잠금이 해제된 상태입니다."),

  CUE_SHEET_ITEM_MENO_NOT_EXIST("존재하지 않는 큐시트 아이템 메모입니다."),

  CUE_SHEET_ITEM_INSERT_ERROR("큐시트아이템 삽입 시 에러가 발생했습니다."),

  APM_SEND_ONLY_WHEN_CUE_SHEET_IN_PROGRESS("큐시트 상태가 작성중일 때만 APM 전송이 가능합니다."),
  APM_SEND_ONLY_WHEN_CUE_SHEET_PREPARING("큐시트 상태가 준비중일 때만 APM 전송이 가능합니다."),
  UNABLE_TRANSMIT_TRSVR_DURING_BROADCAST("방송중에서 트랜잭션 서버로 전송할 수 없습니다."),
  CUE_SHEET_ITEM_MOVED_TO_THE_WRONG_POSITION("큐시트 아이템 이동 위치가 잘못되었습니다."),
  CUE_SHEET_ITEM_CANNOT_BE_SENT_IN_ONAIR_OR_STANDBY("큐시트 아이템의 상태가 ONAIR/STANDBY에서 전송이 되지 않습니다."),
  CAN_MOVE_CUE_SHEET_IN_DRAFT_NOT_EDITING("이동시키고자 하는 큐시트의 상태가 작성중이면서 편집상태가 아닌 경우만 가능합니다."),
  CAN_COPY_CUE_SHEET_IN_DRAFT_NOT_EDITING("복사시키고자 하는 큐시트의 상태가 작성중이면서 편집상태가 아닌 경우만 가능합니다."),
  CAN_ADD_CUE_SHEET_ITEM_IN_PROGRESS_OR_PREPARING("[작성중/준비중] 상태에서만 큐시트아이템 등록이 가능합니다."),
  CAN_MOVE_CUE_SHEET_ITEM_IN_PROGRESS_OR_PREPARING("[작성중/준비중] 상태에서만 큐시트아이템 이동이 가능합니다."),
  CAN_EDIT_CUE_SHEET_IN_PROGRESS_OR_PREPARING("[작성중/준비중] 상태에서만 수정할 수 있습니다."),
  CAN_EDIT_CUE_SHEET_ITEM_IN_PROGRESS_OR_PREPARING_OR_READY("[작성중/준비중/방송대기] 상태에서만 수정할 수 있습니다."),
  CAN_DELETE_CUE_SHEET_ITEM_IN_PROGRESS_OR_PREPARING("큐시트의 상태가 [작성중/준비중]인 경우만 아이템을 삭제할 수 있습니다."),
  CAN_NOT_ADD_CUE_SHEET_ITEM_IN_COMPLETE_CUE_SHEET("[방송완료] 상태이므로 큐시트 아이템을 등록할 수 없습니다."),
  CAN_NOT_MOVE_CUE_SHEET_ITEM_IN_COMPLETE_CUE_SHEET("[방송완료] 상태이므로 큐시트 아이템을 이동할 수 없습니다."),
  CAN_NOT_EDIT_CUE_SHEET_ITEM_IN_ON_AIR_OR_COMPLETE_CUE_SHEET(
      "[방송중/방송완료] 상태이므로 큐시트 아이템을 수정할 수 없습니다."),
  CAN_NOT_VIEW_CUE_SHEET_CAUSE_CUE_TIME("열람이 허용되지 않은 큐시트입니다."),

  // 출장
  PLEASE_SELECT_BUSINESS_TRIP_TYPE("출장 타입을 선택해주세요."),
  PLEASE_ENTER_BUSINESS_TRIP_DATE("출장 일시를 입력해주세요."),
  BUSINESS_TRIP_TYPE_CAN_ONLY_BE_SELECTED_FOR_BUSINESS_TRIP("출장 타입은 출장일 경우에만 선택 가능합니다."),
  BUSINESS_TRIP_DATE_CAN_ONLY_BE_ENTERED_FOR_BUSINESS_TRIP("출장 일자는 출장일 경우에만 입력 가능합니다."),

  // 댓글
  COMMENT_INFO_NOT_EXIST("존재하지 않는 댓글 정보입니다."),
  NOT_EXIST_OR_NOT_MY_COMMENT("존재하지 않거나 내 댓글이 아닙니다."),
  NOT_AUTHORIZED_TO_DELETE_COMMENT("본인이 작성한 댓글만 삭제할 수 있습니다."),

  // 방송아이콘
  SYMBOL_NOT_EXIST("존재하지 않는 방송아이콘입니다."),

  // 게시글
  POST_INFO_NOT_EXIST("존재하지 않는 게시글 정보입니다."),
  NOT_EXIST_OR_NOT_MY_POST("존재하지 않거나 내 게시글이 아닙니다."),

  KAKAO_PARTNER_SERVICE_NOT_AVAILABLE("이용 가능한 카카오 제휴 서비스가 없습니다."),
  NAVER_SPELLCHECK_API_FAILED("네이버 맞춤법 검사기 API 호출에 실패했습니다."),
  EXTERNAL_PORTAL_NOT_AVAILABLE("사용 가능한 외부포털이 없습니다."),
  UNAVAILABLE_PORTAL_TRANSMISSION_SERVICE("포털 전송 서비스를 이용할 수 없습니다"),

  //기자실 게시물
  REPORTER_POST_NOT_EXIST("요청하신 게시물을 찾을 수 없습니다."),
  REPORTER_POST_NOT_DELETED("삭제되지 않은 게시물입니다."),
  REPORTER_COMMENT_NOT_EXIST("해당 댓글을 찾을 수 없습니다."),
  COMMENT_DEPTH_EXCEED("답글은 더 이상 작성할 수 없습니다."),
  CAN_NOT_CHANGE_OBJECT("작성자만 수정할 수 있습니다."),
  NOT_COMMENT_BELONG_BOARD("해당 게시물의 댓글이 아닙니다."),
  COMMENT_LIKE_NOT_EXIST("좋아요 정보를 찾을 수 없습니다."),
  COMMENT_LIKE_ALREADY_EXIST("이미 좋아요를 누른 댓글입니다."),
  INPUTR_CAN_NOT_REACT("본인이 작성한 댓글에는 반응할 수 없습니다."),
  REACTION_STATUS_CAN_NOT_FIND("반응 정보를 찾을 수 없습니다."),
  EXPIRE_PUBLIC_DATE("공개 기간이 아닙니다."),
  REACTION_TYPE_CAN_ONLY_0or1("잘못된 반응 유형입니다."),


  //스크롤
  CANNOT_REVERT_APPROVED_TO_WRITING("승인된 초안 스크롤은 작성중 상태로 되돌릴 수 없습니다."),
  CANNOT_REVERT_WRITING_TO_APPROVED("작성중인 초안 스크롤은 승인 상태로 변경할 수 없습니다."),
  CANNOT_REVERT_SCROLL_STATUS("스크롤 상태는 수정 할 수 없습니다. 신규 작성을 이용해주세요."),
  FINAL_SCROLL_CANNOT_REVERT_COMPLETED("최종 스크롤은 작성완료 상태로 변경할 수 없습니다."),
  FINAL_SCROLL_ONLY_WRITING_STATUS("최종 스크롤은 작성중 상태로만 생성할 수 있습니다. 작성 완료나 승인 상태로는 생성할 수 없습니다."),
  CANNOT_CREATE_APPROVED_SCROLL("스크롤 생성 시에는 승인 상태로 바로 생성할 수 없습니다. 작성 완료 후 승인해 주세요."),
  SCROLL_NOT_EXIST_OR_NOT_APPROVED("최종 스크롤을 찾을 수 없거나 승인되지 않은 스크롤입니다."),
  SCROLL_NOT_EXIST_IN_DATE_RANGE("선택한 기간에 승인된 스크롤이 없습니다."),
  SCROLL_NOT_EXIST("요청하신 스크롤을 찾을 수 없습니다."),
  SCROLL_INFO_NOT_FOUND("스크롤 수정 정보를 찾을 수 없습니다. 새로고침 후 다시 시도해 주세요."),
  SCROLL_LOCK_ERROR("스크롤 잠금 오류"),
  AGG_CLASS_INVALID("유효하지 않은 AGG_CLASS 값"),
  ONLY_INPUTR_CAN_DELETE("작성자만 삭제할 수 있습니다."),
  SCROLL_LOCKED("현재 스크롤이 잠겨있습니다."),
  SCROLL_LOCKED_BY_ANOTHER_USER_CAN_NOT_EDIT("다른 사용자의 잠금으로 인해 수정할 수 없습니다."),
  SCROLL_ALREADY_UNLOCK("이미 잠금 해제된 스크롤입니다."),
  DESKING_ALREADY_EXIST("데스킹이 이미 등록되어 있습니다."),
  ONLY_CHAGR_CAN_ACCESS("데스킹 담당자만 접근할 수 있습니다."),
  ONLY_ACCEPTED_SCROLL_CAN_TRANSFER("승인된 스크롤만 전송할 수 있습니다."),

  //민방 게시판
  COMMERCIAL_BOARD_POST_NOT_DELETED("삭제되지 않은 민방 게시판 게시물 입니다."),
  COMMERCIAL_BOARD_POST_NOT_EXIST("요청하신 게시물을 찾을 수 없습니다."),
  ACCESS_USER_NOT_PERMITTED("접근 권한이 없습니다."),
  COMMENT_NOT_EXIST("해당 댓글을 찾을 수 없습니다."),
  ONLY_ARTICLE_CAN_TRANSFER("기사만 공용으로 이관할 수 있습니다."),

  // 제보
  INCORRECT_PASSCODE("입력하신 패스코드가 올바르지 않습니다."),
  REPORT_NOT_FOUND("요청하신 제보를 찾을 수 없습니다."),
  REPORT_NOT_DELETED("해당 제보는 아직 삭제되지 않았습니다. 페이지를 새로고침한 후 다시 확인해 주세요."),
  REPORT_FILE_INFO_NOT_FOUND("제보 파일 정보를 찾을 수 없습니다."),
  ALREADY_PROCESSED_REPORT("이 제보 파일은 이미 처리되었습니다."),
  EXIST_TRANSFERRED_DATA("이미 이관된 제보는 검토를 취소할 수 없습니다."),
  ONLY_EXAMINER_CANCEL("검토자만 검토를 취소할 수 있습니다."),
  REPORT_NOT_YET_EXAMINED("아직 검토되지 않은 제보입니다."),
  NEWS_NET_REPORT_RECEIVE_FAIL("뉴스넷 제보 수신 중 오류가 발생했습니다."),
  REPORT_COMMENT_NOT_EXIST("해당 댓글을 찾을 수 없습니다."),

  //외신
  WIRE_NEWS_NOT_EXIST("존재하지 않는 외신 입니다."),
  WIRE_NEWS_ALREADY_EXIST("이미 존재하는 외신입니다. 수정을 통해 입력해주세요"),
  WIRE_NEWS_IS_TRANSLATING("번역이 진행중입니다. 잠시후 다시 시도해주세요"),
  WIRE_NEWS_TRANSLATION_FAILED("번역 요청이 실패했습니다."),

  //북한/외신
  NORTH_FOREIGN_NEWS_NOT_EXIST("존재하지 않는 북한/외신 입니다."),
  NORTH_FOREIGN_STT_IS_REQUESTING("번역이 진행중입니다. 잠시후 다시 시도해주세요"),
  NORTH_FOREIGN_CAN_NOT_ARCHIVE("이미 아카이브 되었거나, 할 수 없는 상태입니다."),


  // 취재
  NEWS_SOURCE_INFO_NOT_EXIST("존재하지 않는 취재원 정보입니다."),
  COVERAGE_INFO_NOT_EXIST("존재하지 않는 취재 정보입니다."),

  // 의뢰 공통
  CAN_NOT_COMPLETE_REQUEST("의뢰를 완료할 수 없는 작업 상태입니다."),
  CAN_NOT_UPDATE_REQUEST("의뢰를 수정할 수 없는 작업 상태입니다."),
  CAN_NOT_DELETE_REQUEST("의뢰를 삭제할 수 없는 작업 상태입니다."),
  USER_CAN_NOT_DELETE_REQUEST("본인의 의뢰만 삭제할 수 있습니다."),
  CAN_NOT_CANCEL_REQUEST("의뢰를 취소할 수 없는 작업 상태입니다."),
  USER_CAN_NOT_CANCEL_REQUEST("본인의 작업만 취소할 수 있습니다."),
  CAN_NOT_CONFIRM_BECAUSE_FINISH("이미 승인 절차가 마무리 되었습니다."),
  CAN_NOT_FINISH_REQUEST("요청을 처리할 수 없는 작업 상태입니다."),
  CAN_NOT_FINISH_CONFIRM("요청을 처리할 수 없습니다."),
  CAN_NOT_CHECK_APPROVE("내부 확인을 진행할 권한이 없습니다."),
  CAN_NOT_REVIEW_APPROVE("기자 확인을 진행할 권한이 없습니다."),
  CAN_NOT_WORKING_REQUEST("의뢰를 작업시작할 수 없는 상태입니다."),
  CAN_NOT_DEAD_LINE_REQUEST("영상편집이 작업중이거나 완료상태입니다.\n완료요청날짜를 변경할 수 없습니다."),
  REF_VID_EDL_NOT_EXIST("존재하지 않는 참조영상/EDL번호가 포함되어 있습니다"),

  // CG의뢰
  CAN_NOT_DELETE_REQUEST_GRAPHIC_HAVING_ITEMS("하위 CG의뢰가 존재하므로 삭제하실 수 없습니다."),
  REQUEST_GRAPHIC_NOT_EXIST("존재하지 않는 CG 의뢰입니다."),
  CAN_ASSIGN_WORKER_BEFORE_REVIEW("작업자를 재배정할 수 없습니다."),
  REQUEST_GRAPHIC_ITEM_NOT_EXIST("존재하지 않는 CG의뢰 아이템입니다."),
  ONLY_REJECTED_MEDIA_CAN_DISABLE("반려된 미디어만 비활성화시킬 수 있습니다."),
  MUST_BE_AT_LEAST_1("요청 건수는 1 이상이여야 합니다."),

  // 그래픽 템플릿
  GRAPHIC_TEMPLATE_NOT_EXIST("존재하지 않는 그래픽 템플릿입니다."),
  GRAPHIC_TEMPLATE_ORDER_OUT_OF_RANGE("그래픽 템플릿의 인덱스 범위를 초과합니다. "),

  // 그래픽 템플릿 아이템
  GRAPHIC_TEMPLATE_ITEM_NOT_EXIST("존재하지 않는 그래픽 템플릿 아이템입니다."),
  GRAPHIC_TEMPLATE_ITEM_ORDER_OUT_OF_RANGE("그래픽 템플릿 아이템의 인덱스 범위를 초과합니다. "),


  // 데스킹
  APPROVE_PROCESS_ALREADY_EXIST("같은 승인 프로세스가 이미 존재합니다"),
  APPROVE_NOT_EXIST("승인 프로세스가 존재하지 않습니다."),
  PREVIOUS_STAGE_NOT_APPROVED("이전 단계의 데스킹이 승인되지 않았습니다."),
  MAX_APPROVAL_LEVEL_FOUR("승인은 4단계까지만 설정이 가능합니다."),
  USER_CAN_NOT_CANCEL_DESKING("데스킹 요청자만 취소할 수 있습니다."),
  APPROVER_IDS_IS_MISSING("승인자 정보가 누락되었습니다."),
  CHECK_APPROVAL_ID("승인 아이디를 확인해주세요."),
  REQUESTER_ONLY_CAN_DELETE("승인 요청자만 삭제 할 수 있습니다."),
  STEP_AUTHORIZED_APPROVER("해당 승인단계에 권한이 있는 승인자만 처리할 수 있습니다."),
  ALREADY_APPROVED_OR_REJECTED("데스킹이 이미 최종승인 되었습니다."),
  APPROVAL_IS_MISSING("승인 요청 정보가 누락되었습니다."),
  CAN_NOT_INCLUDE_SELF_IN_REQUEST_LIST("본인 요청을 제외한 데스킹 요청에는 자신을 포함 할 수 없습니다."),
  APPROVAL_STATE_NOT_EXIST("해당 승인 요청 상태가 존재 하지 않습니다. - {승인 또는 거절} "),

  // 파일
  FILE_NOT_EXIST("존재하지 않는 파일입니다."),
  FILE_DIVISION_CODE_IS_REQUIRED("파일 구분 코드는 필수입니다."),
  FILE_EXTENSION_NOT_FOUND("파일 확장자를 찾을 수 없습니다."),
  FILE_UPLOAD_FAIL("파일 업로드에 실패하였습니다."),
  FILE_DELETE_FAIL("파일 삭제에 실패하였습니다."),

  // 일정
  SCHEDULE_INFO_NOT_EXIST("존재하지 않는 일정 정보입니다."),
  NOT_EXIST_OR_NOT_MY_SCHEDULE("존재하지 않거나 내 일정 정보가 아닙니다."),

  //스포츠 일정
  SPORTS_SCHEDULE_INFO_NOT_EXIST("스포츠 일정 게시물을 찾을 수 없습니다."),
  SPORTS_SCHEDULE_ITEM_INFO_NOT_EXIST("스포츠 일정 정보를 찾을 수 없습니다."),
  NAVER_ITEM_CAN_NOT_DELETE("네이버에서 수신한 일정은 삭제할 수 없습니다."),
  NQC_REQUESTER_NOT_FOUND("회선 신청자 정보를 찾을 수 없습니다."),
  NQC_REQUESTER_COOPERATION_NOT_FOUND("회선 신청자의 회사 정보를 찾을 수 없습니다."),
  NQC_ALREADY_EXIST("이미 회선 신청이 등록되어 있습니다."),
  NQC_FAILED("Wise 회선 신청중 오류가 발생했습니다. SBS Wise 에 문의 해주세요"),
  NQC_READ_FAILED("Wise 회선 조회중 오류가 발생했습니다. SBS Wise 에 문의 해주세요"),
  //긴급 공지
  URGENT_NOTICE_NOT_EXIST("긴급공지를 찾을 수 없습니다."),

  // 기획서
  PLAN_NOT_FOUND("존재하지 않는 기획서입니다."),
  PLAN_ONLY_CREATOR_EDIT("Only the notification creator is edited."),
  PLAN_ONLY_CREATOR_DELETE("Only the notification creator is deleted."),
  PLAN_USER_NOT_FOUND("User not found with Id: "),
  PLAN_USER_NOT_PERMISSION("You do not have permission with this plan"),
  PLAN_CMNT_PARENT_NOT_FOUND("Parent comment not found."),
  PLAN_CMNT_NOT_FOUND_BY_PLANID_CMNTID("Comment not found for given plan and comment ID."),
  PLAN_CMNT_NOT_FOUND("Plan comment not found."),
  PLAN_CMNT_ONLY_CREATOR_EDIT("Only the comment creator is edited."),
  PLAN_CMNT_ONLY_CREATOR_DELETE("Only the comment creator is deleted."),
  PLAN_CMNT_LIMIT_LEVEL("Replies are limited to 2 levels."),

  PLEASE_CHECK_MOVE_RANGE("이동 범위를 확인해 주세요."),
  MULTIPLE_VIDEO_TRANSMISSION_NOT_SUPPORTED("다중 동영상 전송 기능은 지원되지 않습니다."),
  VIDEO_JOURNALIST_CAN_ONLY_BE_ENTERED_ON_ASSIGNMENT("영상 기자는 취재의뢰일 경우에만 입력 가능합니다."),
  DIFFERENT_AREA_TRANSMISSION("타 지역사 송고는 다른 지역으로 송고하는 기능입니다."),
  CANNOT_EDIT("편집 할 수 없습니다."),
  CANNOT_ENTER_BLANK_ONLY("공백만 입력할 수 없습니다."),
  VERIFY_START_AND_END_DATE("검색 시작일과 검색 종료일을 확인하세요."),
  MATCHING_CODE_NOT_FOUND("일치하는 코드가 없습니다."),
  LINK_NOT_EXIST("해당 링크가 없습니다."),
  CHANNEL_INFO_NOT_MATCH("채널 정보가 일치하지 않습니다."),
  SYMBOL_NOT_FOUND("해당 방송아이콘을 찾을 수 없습니다."),
  VERIFY_SEARCH_DATE_TYPE("검색 날짜 타입을 확인하세요."),
  SYMBOL_ALREADY_EXISTS("이미 등록된 방송 아이콘 코드값입니다."),


  LOG_INFO_NOT_EXIST("존재하지 않는 로그 정보입니다."),
  REGIONAL_INFO_NOT_EXIST("존재하지 않는 지역사 정보입니다."),
  CATEGORY_INFO_NOT_EXIST("존재하지 않는 카테고리 정보입니다."),
  PROGRAM_CREATE_FAIL("프로그램 코드 생성 중 오류가 발생하였습니다."),
  PROGRAM_NOT_EXIST("존재하지 않는 프로그램 입니다."),
  DEPARTMENT_NOT_EXIST("존재하지 않는 부서입니다."),
  CHANNEL_NOT_EXIST("존재하지 않는 채널입니다."),
  TAG_NOT_EXIST("존재하지 않는 태그입니다."),
  ID_NOT_EXIST("존재하지 않는 ID 입니다."),
  PERMISSION_NOT_EXIST("존재하지 않는 권한 입니다."),
  SCROLL_NEWS_CATEGORY_NOT_EXIST("존재하지 않는 하단롤 카테고리 정보입니다."),
  NOTICE_INFO_NOT_EXIST("존재하지 않는 공지사항 정보입니다."),
  GROUP_INFO_NOT_EXIST("존재하지 않는 그룹 정보입니다."),
  BOARD_INFO_NOT_EXIST("존재하지 않는 게시판 정보 입니다."),
  BOARD_MEMO_FIELD_TEMPLATE_NOT_EXIST("존재하지 않는 게시판 필드 템플릿 입니다."),
  BOARD_MEMO_FIELD_NOT_EXIST("존재하지 않는 게시판 필드입니다."),

  // 맞춤법 검사기
  SPELL_CHECKER_OPEN_FAIL("맞춤법 검사기를 여는데 실패하였습니다."),

  // 컬럼 개인화 정보
  COLUMN_PERSONALIZATION_INFO_NOT_EXIST("컬럼 개인화 정보가 존재하지 않습니다."),
  COLUMN_PERSONALIZATION_INFO_ALREADY_EXIST("중복되는 컬럼 개인화 정보가 존재합니다."),

  // 영상편집 의뢰
  REQUEST_VIDEO_NOT_EXIST("영상편집의뢰 정보가 존재하지 않습니다."),
  EDITOR_ALREADY_ASSIGNED("해당 영상 의뢰에 이미 배정된 편집자입니다."),
  EDITOR_NOT_ASSIGNED("편집자가 배정되지 않았습니다."),
  CAN_ASSIGN_EDITOR_BEFORE_START_EDIT("편집자를 재배정할 수 없습니다."),
  CAN_NOT_DELETE_COMPLETE_REQUEST_VIDEO("완료된 영상편집 의뢰는 삭제할 수 없습니다."),
  SEGMENT_INFORMATION_NOT_EXIST("구간 정보가 존재하지 않습니다."),
  EDITOR_AND_ACTOR_NOT_MATCH("배정된 담당자 정보와 일치하지 않습니다."),
  NO_MEDIA_TO_COMPLETE("완료 처리 가능한 미디어가 없습니다."),
  SOME_MEDIA_IS_UPLOADING("등록중인 미디어가 있습니다."),
  CAN_NOT_COMPLETE("편집중 또는 완료 상태가 아니므로 처리할 수 없습니다."),
  FAILED_TO_COMPLETE("완료 처리에 실패하였습니다."),

  // 영상취재 의뢰
  COVERAGE_SCHEDULE_NOT_EXIST("존재하지 않는 영상취재 의뢰입니다."),
  DELETE_COVERAGE_SCHEDULE_NOT_PERMITTED("해당 영상취재 의뢰 삭제 권한이 없습니다."),
  UPDATE_COVERAGE_SCHEDULE_NOT_PERMITTED("해당 영상취재 의뢰 수정 권한이 없습니다."),
  ALREADY_REGISTERED_ROUTER_NUMBER("이미 등록된 라우터번호입니다."),
  ALREADY_REGISTERED_TAG("이미 등록된 머리글입니다."),
  ALREADY_REGISTERED_ASSET_NUMBER("이미 등록된 자산번호입니다."),
  MNG_ID_NOT_EXIST("존재하지 않는 MNG 아이디가 포함되어 있습니다"),
  ASSIGNED_MEDIA_CANNOT_BE_DELETED("영상배정된 자료입니다.\n삭제할 수 없습니다."),
  TAG_ID_NOT_EXIST("존재하지 않는 머리글 아이디가 포함되어 있습니다"),

  // MQ 메세지
  PLEASE_CHECK_MESSAGE_TYPE("메세지 타입을 확인해 주세요."),
  MESSAGE_SEND_FAIL("메세지 발송에 실패하였습니다."),
  CREATE_QUEUE_FAIL("큐 생성에 실패하였습니다."),
  EXPIRE_TIME_CHECK("만료시간을 확인해 주세요."),

  // 알림
  ALERT_NOT_EXIST("존재하지 않는 알림이 있습니다."),
  ALERT_NOT_UNIQUE("중복된 알림이 존재합니다."),

  // 모바일 기기 관리
  SOME_MOBILE_DEVICE_ALREADY_EXIST("이미 등록된 기기가 있습니다."),
  THIS_MOBILE_DEVICE_ALREADY_EXIST("이미 등록된 기기입니다."),
  MOBILE_DEVICE_NOT_EXIST("등록된 기기가 없습니다."),

  // 사용자 팝업 알림 관리
  USER_POPUP_ALERT_INFO_NOT_EXIST("사용자의 설정 정보가 존재하지 않습니다."),
  MUTE_TIME_INVALID("시작 시간(Hour)이 종료 시간(Hour)과 같습니다."),

  // 관리자 팝업 알림 관리
  MANAGER_POPUP_ALERT_INFO_NOT_EXIST("설정 대상의 정보가 존재하지 않습니다."),
  NOT_FOUND_CORRECT_MANAGER_POPUP_ALERT_INFO("해당 팝업 알림 관리 정보를 찾을 수 없습니다."),

  // Shared API
  INVALID_SECRET_KEY("SecretKey가 일치하지 않습니다."),
  INVALID_DATE("종료일자가 시작일자보다 이전입니다."),
  ALREADY_EXIST("이미 등록된 정보가 있는지 확인해주세요."),
  DUPLICATE_KEY("중복된 입력값이 존재합니다."),


  //External API
  INVALID_FUN_FLAG("올바르지 않은 fun flag 값 입니다."),

  // 인제스트
  INGEST_REQUEST_NOT_EXIST("인제스트 의뢰가 존재하지 않습니다."),

  // MAM 연계 API
  MAM_READ_FAIL("정보를 가져올 수 없습니다."),
  MAM_WRITE_FAIL("정보를 저장할 수 없습니다."),
  MAM_INFO_NOT_FOUND("정보를 찾을 수 없습니다."),
  MAM_REQUEST_NOT_PROCESSABLE("요청을 처리할 수 없습니다."),
  MEDIA_STATUS_CANNOT_DELETE("미디어 상태가 삭제할 수 없는 상태입니다."),
  REQUEST_MEDIA_STATUS_CANNOT_DELETE("미디어 의뢰 상태가 삭제할 수 없는 상태입니다."),
  CAN_NOT_REGISTER_FILE("파일을 등록할 수 없습니다."),
  MAM_RETRY_FAIL("CMS API 호출 시 예기치 못한 에러가 발생하였습니다."),

  // SAM 연계 API
  SAM_READ_FAIL("정보를 가져올 수 없습니다."),

  // 미디어
  MEDIA_NOT_EXIST("존재하지 않는 미디어입니다."),
  MEDIA_ALREADY_EXIST("이미 존재하는 미디어입니다."),
  CAN_NOT_DELETE_MEDIA("미디어를 삭제할 수 없는 상태입니다."),
  MEDIA_ARTICLE_ALREADY_EXIST("이미 매핑된 콘텐츠입니다."),

  // 검색어
  KEYWORD_NOT_EXIST("존재하지 않는 최근 검색어입니다."),

  // 메일
  MAIL_SEND_FAIL("메일 발송에 실패하였습니다."),

  // VIZRT 연계 API
  VIZRT_READ_FAIL("vizrt 정보를 가져올 수 없습니다."),
  VIZRT_WRITE_FAIL("vizrt 정보를 저장할 수 없습니다."),
  VIZRT_TEMPLATE_NOT_EXIST("존재하지 않는 vizrt 템플릿입니다."),
  VIZRT_TEMPLATE_LIST_SYNC_FAIL("vizrt 목록 동기화에 실패했습니다."),

  // XML 파싱
  PLAYLIST_SEND_FAIL("플레이리스트 전송에 실패했습니다."),

  // VDC - developer
  NOTI_ONLY_CREATOR_EDIT("공지사항 작성자만 수정할 수 있습니다."),
  NOTI_ONLY_CREATOR_DELETE("공지사항 작성자만 삭제할 수 있습니다."),
  NOTI_NOT_FOUND("공지사항을 찾을 수 없습니다: "),
  NOTI_USER_NOT_FOUND("사용자를 찾을 수 없습니다: "),
  NOTI_USER_NOT_PERMISSION("해당 공지사항에 대한 권한이 없습니다."),
  NOTI_CMNT_PARENT_NOT_FOUND("상위 댓글을 찾을 수 없습니다."),
  NOTI_CMNT_NOT_FOUND_BY_NOTIID_CMNTID("해당 공지사항의 댓글을 찾을 수 없습니다."),
  NOTI_CMNT_NOT_FOUND("공지사항 댓글을 찾을 수 없습니다."),
  NOTI_CMNT_ONLY_CREATOR_EDIT("댓글 작성자만 수정할 수 있습니다."),
  NOTI_CMNT_ONLY_CREATOR_DELETE("댓글 작성자만 삭제할 수 있습니다."),
  NOTI_CMNT_LIMIT_LEVEL("답글은 2단계까지만 가능합니다."),
  NOTI_VOTE_NOT_FOUND("투표를 찾을 수 없습니다: "),
  NOTIT_VOTE_OPTION_NOT_FOUND("투표 옵션을 찾을 수 없습니다: "),
  NOTI_PIN_MAX_10("고정 공지사항은 최대 10개까지 가능합니다."),
  NOTI_COMMENT_DELETED_MESSAGE("작성자에 의해 삭제된 댓글입니다."),

  BULLETINBB_POST_ONLY_CREATOR_EDIT("게시물 작성자만 수정할 수 있습니다."),
  BULLETINBB_POST_ONLY_CREATOR_DELETE("게시물 작성자만 삭제할 수 있습니다."),
  BULLETINBB_ONLY_CREATOR_EDIT("게시판 작성자만 수정할 수 있습니다."),
  BULLETINBB_ONLY_CREATOR_DELETE("게시판 작성자만 삭제할 수 있습니다."),
  BULLETINBB_NOT_FOUND("게시판을 찾을 수 없습니다: "),
  BULLETINBB_USER_NOT_FOUND("사용자를 찾을 수 없습니다: "),
  BULLETINBB_USER_NOT_PERMISSION("해당 게시판에 대한 권한이 없습니다."),
  BULLETINBB_POST_CMNT_PARENT_NOT_FOUND("상위 댓글을 찾을 수 없습니다."),
  BULLETINBB_POST_CMNT_NOT_FOUND_BY_BULLETINBBID_CMNTID("해당 게시판의 댓글을 찾을 수 없습니다."),
  BULLETINBB_POST_CMNT_NOT_FOUND("게시판 댓글을 찾을 수 없습니다."),
  BULLETINBB_POST_CMNT_ONLY_CREATOR_EDIT("댓글 작성자만 수정할 수 있습니다."),
  BULLETINBB_POST_CMNT_ONLY_CREATOR_DELETE("댓글 작성자만 삭제할 수 있습니다."),
  BULLETINBB_POST_CMNT_LIMIT_LEVEL("답글은 2단계까지만 가능합니다."),
  BULLETINBB_POST_INFO_NOT_EXIST("게시물이 존재하지 않습니다."),
  BULLETINBB_INFO_NOT_EXIST("게시판이 존재하지 않습니다."),
  BULLETINBB_EVENT_NOT_FOUND("게시판 이벤트를 찾을 수 없습니다: "),
  BULLETINBB_DUPLICATE_PERMISSION("중복된 권한 정보입니다: "),
  BULLETINBB_CONNECT_NOT_FOUND("게시판 연결 정보를 찾을 수 없습니다: "),
  BULLETIN_INVALID_BOARD_TYPE("잘못된 게시판 유형입니다: "),
  BULLETIN_INVALID_CONNECT_NAME("잘못된 연결명입니다: "),
  BULLETINBB_APPRV_REQUEST("현재 게시판 상태에서는 요청할 수 없습니다: "),
  BULLETINBB_APPRV_APPROVAL("작성중, 승인됨, 반려 상태에서는 승인할 수 없습니다."),
  BULLETINBB_APPRV_PENDING("요청 상태에서만 대기로 변경할 수 있습니다."),
  BULLETINBB_APPRV_RETURN("요청 상태에서만 반려로 변경할 수 있습니다."),
  BULLETINBB_NOT_FOUND_APPROVER("승인자를 찾을 수 없습니다."),
  BULLETINBB_NOT_FOUND_REQUESTER("요청자를 찾을 수 없습니다."),
  BULLETINBB_STATUS_NOT_FOUND("게시판 상태를 찾을 수 없습니다: "),
  BULLETINBB_APPRV_DELETE("승인된 게시판은 삭제할 수 없습니다."),
  BULLETINBB_APPRV_EDIT("승인된 게시판은 수정할 수 없습니다."),
  BULLETINBB_POST_NOT_FOUND("게시물을 찾을 수 없습니다: "),
  BULLETINBB_NOT_APPROVAL("승인되지 않은 게시판입니다."),
  BULLETINBB_PERMISSION_DENIED("해당 게시판에 대한 권한이 없습니다."),
  BULLETINBB_REQUEST_APPROVAL_TEXT("승인을 요청합니다."),

  SEARCH_DATE_IS_MISSING("검색일자가 누락되었습니다."),

  // 기타
  DATE_FORMAT_IS_INVALID("날짜 형식이 올바르지 않습니다."),

  CUE_ITEM_MAPPING_NOT_SENT("큐시트아이템의 방송상태가 준비중이 아닌 아이템에 기사가 매핑되어있습니다. 매핑정보가 트랜잭션 서버로 전송되지 않습니다."),

  // 편집자배정 관리
  EDITOR_ASSIGNMENT_NOT_EXIST("존재하지 않는 편집자배정관리 정보입니다."),

  // SUPER CLIENT 연계 API
  // MAM 연계 API
  SUPER_READ_FAIL("정보를 가져올 수 없습니다."),

  // STT
  TTS_SETTING_NEED_CHECK("TTS 설정 확인이 필요합니다."),
  TTS_SERVER_CONNECTION_FAIL("TTS 서버와의 통신에 실패하였습니다."),
  TTS_REQUEST_FAIL("TTS 요청을 처리하는데 실패하였습니다."),
  ;

  private final String message;

  public String getMessage(String message) {
    return Optional.ofNullable(message)
        .filter(Predicate.not(String::isBlank))
        .orElse(this.getMessage());
  }
}
