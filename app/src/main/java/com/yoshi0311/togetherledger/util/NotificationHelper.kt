package com.yoshi0311.togetherledger.util

object NotificationHelper {

    fun extractKbBankAmount(content: String): Int {
        // 패턴: [출금 900원] 또는 [입금 1,000원]
        // (입금|출금)\s+([\d,]+)원  => '입금' 혹은 '출금' 뒤의 숫자와 콤마를 찾음
        val regex = Regex("""\[(?:입금|출금)\s+([\d,]+)원\]""")
        val matchResult = regex.find(content)

        return matchResult?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull() ?: 0
    }

    fun extractKbBankContent(rawContent: String): String {
        // 1. 날짜/시간 패턴 찾기 (MM/dd HH:mm 형식)
        val dateTimeRegex = Regex("""\d{2}/\d{2} \d{2}:\d{2}""")
        val matchResult = dateTimeRegex.find(rawContent) ?: return rawContent.substringBefore(" ")

        // 2. 날짜/시간 바로 뒤부터 문자열 자르기
        // 예: " 343602-**-***066 오오? 스마트폰출금 900 잔액7,343,000"
        val afterDateTime = rawContent.substring(matchResult.range.last + 1).trim()

        // 3. 계좌번호 길이만큼 건너뛰기
        // 첫 번째 공백(계좌번호와 본문 사이)을 기준으로
        val firstSpaceAfterAcc = afterDateTime.indexOf(" ")
        if (firstSpaceAfterAcc == -1) return afterDateTime

        val onlyContentAndAmount = afterDateTime.substring(firstSpaceAfterAcc).trim()

        // 4. 뒷부분(금액, 잔액) 제거
        // 본문 뒤에는 항상 숫자가 나오므로, 첫 번째 숫자가 등장하기 전까지만 가져옵니다.
        val midContent = onlyContentAndAmount.split(Regex("""\s+\d""")).firstOrNull() ?: onlyContentAndAmount

        val words = midContent.split(" ").filter { it.isNotBlank() }

        val finalContent = if (words.size > 1) {
            // 마지막 요소(스마트폰출금)를 제외한 나머지만 다시 공백으로 합침
            words.dropLast(1).joinToString(" ")
        } else {
            // 단어가 하나뿐이라면(예: "오오?") 그대로 유지하거나 상황에 맞게 처리
            midContent
        }

        return finalContent.trim()
    }

    fun extractKakaobankAmount(content: String): Int {
        // 패턴: [출금 1,000원] 또는 [입금 500원]에서 숫자만 추출
        val regex = Regex("""\[(?:입금|출금)\s+([\d,]+)원\]""")
        val matchResult = regex.find(content)

        return matchResult?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull() ?: 0
    }

    fun extractKakaobankContent(content: String): String {
        // "→" 기호 이후의 문자열을 가져옴
        val afterArrow = content.substringAfter("→", "").trim()

        // 만약 화살표가 없거나 뒷내용이 비어있다면 전체 내용에서 첫 단어만 반환 (예외 처리)
        return afterArrow.ifEmpty { content.substringBefore(" ") }
    }

    fun extractHanaCardAmount(content: String): Int {
        // 패턴: [(결제) 2,400원] -> 괄호 안의 결제/취소와 금액 추출
        // \([\d,]+\) : 숫자와 콤마를 포함한 금액 그룹
        val regex = Regex("""\[(?:\(\w+\))\s+([\d,]+)원\]""")
        val matchResult = regex.find(content)

        return matchResult?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull() ?: 0
    }

    fun extractHanaCardContent(content: String): String {
        // 1. 대괄호 부분(금액 정보)을 제외한 나머지 문자열을 가져옴
        // content.substringAfter("] ")는 "[...] " 뒤의 내용을 반환
        val afterBracket = content.substringAfter("] ", "").trim()

        // 2. 슬래시(/) 전까지의 내용만 추출
        return afterBracket.substringBefore("/", "").trim()
    }

    fun extractKakaoPayAmount(content: String): Int {
        // 1. 하이픈으로 분리하여 각 줄을 리스트로 만듦
        val lines = content.split("-")

        var orderAmount = 0
        var discountAmount = 0

        for (line in lines) {
            val trimmed = line.trim()
            // 2. 각 라인에서 키워드와 숫자 추출
            when {
                trimmed.contains("주문금액") -> {
                    orderAmount = extractNumber(trimmed)
                }
                trimmed.contains("할인금액") -> {
                    discountAmount = extractNumber(trimmed)
                }
            }
        }

        return orderAmount - discountAmount
    }

    fun extractKakaoPayContent(content: String): String {
        // 하이픈으로 분리 후 "구매처" 라인을 찾아 콜론 뒤의 값 추출
        val lines = content.split("-")
        val storeLine = lines.find { it.contains("구매처") } ?: return "알 수 없는 구매처"

        return storeLine.substringAfter(":").trim()
    }

    // 금액 문자열(예: "12,000원")에서 숫자만 추출하는 도우미 함수
    private fun extractNumber(text: String): Int {
        val regex = Regex("""[\d,]+""")
        return regex.find(text)?.value?.replace(",", "")?.toIntOrNull() ?: 0
    }
}