package com.chatroom.tieba.entity;

import java.util.Date;

public class ForumChatRoom {
    private Long id;
    private Long partitionId;
    private String partitionCode;
    private String partitionName;
    private String roomCode;
    private String roomName;
    private String roomType;
    private String status;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPartitionId() { return partitionId; }
    public void setPartitionId(Long partitionId) { this.partitionId = partitionId; }
    public String getPartitionCode() { return partitionCode; }
    public void setPartitionCode(String partitionCode) { this.partitionCode = partitionCode; }
    public String getPartitionName() { return partitionName; }
    public void setPartitionName(String partitionName) { this.partitionName = partitionName; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
