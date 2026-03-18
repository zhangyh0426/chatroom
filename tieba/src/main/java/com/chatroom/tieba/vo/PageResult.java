package com.chatroom.tieba.vo;

import java.util.List;

/**
 * 通用分页结果封装
 */
public class PageResult<T> {
    private List<T> list;       // 当前页数据
    private int pageNum;        // 当前页号 (从1开始)
    private int pageSize;       // 每页条数
    private int totalCount;     // 总记录数
    private int totalPages;     // 总页数

    public PageResult() {}

    public PageResult(List<T> list, int pageNum, int pageSize, int totalCount) {
        this.list = list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = (totalCount + pageSize - 1) / pageSize;
    }

    public boolean hasPrev() { return pageNum > 1; }
    public boolean hasNext() { return pageNum < totalPages; }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
