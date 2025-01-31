package com.finalproject.mvc.sobeit.service;

import com.finalproject.mvc.sobeit.dto.ExpenditureResponseDTO;
import com.finalproject.mvc.sobeit.entity.Article;
import com.finalproject.mvc.sobeit.entity.Users;
import com.finalproject.mvc.sobeit.repository.ArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService{
    private final ArticleRepo articleRepo;

    /**
     * 월별 지출 내역 가져오기
     * @param user
     * @param year
     * @param month
     * @return Map<날짜, 지출 내역 리스트>
     */
    @Override
    public Map<Integer, List<?>> getExpenditure(Users user, int year, int month) {
        Long userSeq = user.getUserSeq();
        Map<Integer, List<?>> expMap = new HashMap<>();
        // 1일~31일 일별 지출 가져오기
        for(int i=1; i<32;i++) {
            expMap.put(i,getExpenditureDay(userSeq, year, month, i));
        }
        return expMap;
    }

    /**
     * 일별 지출 내역 가져오기
     * @param userSeq
     * @param year
     * @param month
     * @param day
     * @return 해당 날짜에 쓴 지출 내역 리스트
     */
    List<ExpenditureResponseDTO> getExpenditureDay(Long userSeq, int year, int month, int day){
        LocalDate date = LocalDate.of(year, month, day);
        // 유저가 그 날 쓴 지출 글 가져오기
        List<Article> articleList = articleRepo.findExpenditureArticlesByConsumptionDate(userSeq, date);

        // ResponseDTOList로 변환
        List<ExpenditureResponseDTO> expenditureResponseDTOList = new ArrayList<>();
        for (Article article : articleList) {
            // 가계부 메모 가져오기
            String context = article.getFinancialText();
            // 가계부 메모가 없다면 글 내용 가져오기
            if (context == null || context.length()==0){
                context = article.getArticleText();
            }
            // ResponseDTO 생성
            ExpenditureResponseDTO expenditureResponseDTO = ExpenditureResponseDTO.builder()
                    .expenditureCategory(article.getExpenditureCategory())
                    .context(context)
                    .amount(article.getAmount())
                    .articleSeq(article.getArticleSeq())
                    .consumptionDate(article.getConsumptionDate())
                    .build();
            // 리스트에 추가
            expenditureResponseDTOList.add(expenditureResponseDTO);
        }

        return expenditureResponseDTOList;
    }

    /**
     * 월별 차트 가져오기
     * @param year
     * @param month
     * @return Map<카테고리번호 : 지출 금액>
     */
    @Override
    public Map<Integer, Long> getChart(Users user, int year, int month) {
        // 이번달 1일 ~ 다음달 1일 범위 지정
        LocalDate[] date = parseDate(year, month);

        Map<Integer, Long> amountMap= new HashMap<>();

        // 카테고리 1번부터 6번까지 지출 금액 담기
        for(int i=1; i<7; i++){
            amountMap.put(i, articleRepo.findSumAmountByUserSeqAndCategory(user.getUserSeq(), i, date[0], date[1]));
        }
        return amountMap;
    }

    /**
     * 월별 캘린더 가져오기
     * @param user
     * @param year
     * @param month
     * @return Map<날짜 : 금액>
     */
    @Override
    public Map<Integer, Long> getCalendar(Users user, int year, int month) {
        Long userSeq = user.getUserSeq();

        Map<Integer, Long> calMap = new HashMap<>();
        // 1일~31일 일별 지출 가져오기
        for(int i=1; i<32;i++) {
            LocalDate date = LocalDate.of(year, month, i); // 날짜 생성
            calMap.put(i, articleRepo.findSumAmountByConsumptionDate(userSeq, date));
        }
        return calMap;
    }

    /**
     * 월 지출 금액 구하기
     * @param userSeq
     * @param year
     * @param month
     * @return 해당 달 지출 금액 합계
     */
    @Override
    public Long getSumAmount(Long userSeq, int year, int month){
        // 이번달 1일 ~ 다음달 1일 범위 지정
        LocalDate[] date = parseDate(year, month);

        // 이 달 쓴 전체 지출 금액 가져오기
        return articleRepo.findSumAmountByUserSeqAndDate(userSeq, date[0], date[1]);
    }

    /**
     * 날짜 받아서 시작점, 끝점 LocalDate로 parsing
     * @param year
     * @param month
     * @return [0]:시작점, [1]:끝점
     */
    public LocalDate[] parseDate(int year, int month){
        LocalDate start;
        LocalDate end;
        if (month<9){ // 1월~8월
            start = LocalDate.parse(year+"-0"+month+"-01");
            end = LocalDate.parse(year+"-0"+(month+1)+"-01");
        } else if (month ==9){ // 9월은 end에 0 안 붙임
            start = LocalDate.parse(year+"-0"+month+"-01");
            end = LocalDate.parse(year+"-"+(month+1)+"-01");
        }
        else if (month==12){ // 12월은 end가 내년 1월1일
            start = LocalDate.parse(year+"-"+month+"-01");
            end = LocalDate.parse((year+1) +"-01-01");
        } else { // 10월, 11월
            start = LocalDate.parse(year+"-0"+month+"-01");
            end = LocalDate.parse(year+"-"+(month+1)+"-01");
        }
        return new LocalDate[]{start, end};
    }

}
