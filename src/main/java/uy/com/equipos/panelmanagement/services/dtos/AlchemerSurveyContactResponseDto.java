package uy.com.equipos.panelmanagement.services.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlchemerSurveyContactResponseDto {

    @JsonProperty("result_ok")
    private boolean resultOk;

    @JsonProperty("total_count")
    private int totalCount;

    private int page;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("results_per_page")
    private int resultsPerPage;

    private List<AlchemerContactDto> data;

    // Getters and setters

    public boolean isResultOk() {
        return resultOk;
    }

    public void setResultOk(boolean resultOk) {
        this.resultOk = resultOk;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    public List<AlchemerContactDto> getData() {
        return data;
    }

    public void setData(List<AlchemerContactDto> data) {
        this.data = data;
    }
}
