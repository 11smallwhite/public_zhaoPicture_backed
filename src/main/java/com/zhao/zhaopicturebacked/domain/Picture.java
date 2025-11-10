package com.zhao.zhaopicturebacked.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="picture")
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    @TableField(value = "p_url")
    private String pUrl;

    /**
     * 图片名称
     */
    @TableField(value = "p_name")
    private String pName;

    /**
     * 简介
     */
    @TableField(value = "p_introduction")
    private String pIntroduction;

    /**
     * 分类
     */
    @TableField(value = "p_category")
    private String pCategory;

    /**
     * 标签（JSON 数组）
     */
    @TableField(value = "p_tags")
    private String pTags;

    /**
     * 图片体积
     */
    @TableField(value = "p_size")
    private Long pSize;

    /**
     * 图片宽度
     */
    @TableField(value = "p_width")
    private Integer pWidth;

    /**
     * 图片高度
     */
    @TableField(value = "p_height")
    private Integer pHeight;

    /**
     * 图片宽高比例
     */
    @TableField(value = "p_scale")
    private Double pScale;

    /**
     * 图片格式
     */
    @TableField(value = "p_format")
    private String pFormat;

    /**
     * 创建用户 id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 编辑时间
     */
    @TableField(value = "edit_time",fill = FieldFill.INSERT_UPDATE)
    private Date editTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    public Long getId() {
        return id;
    }

    /**
     * id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 图片 url
     */
    public String getpUrl() {
        return pUrl;
    }

    /**
     * 图片 url
     */
    public void setpUrl(String pUrl) {
        this.pUrl = pUrl;
    }

    /**
     * 图片名称
     */
    public String getpName() {
        return pName;
    }

    /**
     * 图片名称
     */
    public void setpName(String pName) {
        this.pName = pName;
    }

    /**
     * 简介
     */
    public String getpIntroduction() {
        return pIntroduction;
    }

    /**
     * 简介
     */
    public void setpIntroduction(String pIntroduction) {
        this.pIntroduction = pIntroduction;
    }

    /**
     * 分类
     */
    public String getpCategory() {
        return pCategory;
    }

    /**
     * 分类
     */
    public void setpCategory(String pCategory) {
        this.pCategory = pCategory;
    }

    /**
     * 标签（JSON 数组）
     */
    public String getpTags() {
        return pTags;
    }

    /**
     * 标签（JSON 数组）
     */
    public void setpTags(String pTags) {
        this.pTags = pTags;
    }

    /**
     * 图片体积
     */
    public Long getpSize() {
        return pSize;
    }

    /**
     * 图片体积
     */
    public void setpSize(Long pSize) {
        this.pSize = pSize;
    }

    /**
     * 图片宽度
     */
    public Integer getpWidth() {
        return pWidth;
    }

    /**
     * 图片宽度
     */
    public void setpWidth(Integer pWidth) {
        this.pWidth = pWidth;
    }

    /**
     * 图片高度
     */
    public Integer getpHeight() {
        return pHeight;
    }

    /**
     * 图片高度
     */
    public void setpHeight(Integer pHeight) {
        this.pHeight = pHeight;
    }

    /**
     * 图片宽高比例
     */
    public Double getpScale() {
        return pScale;
    }

    /**
     * 图片宽高比例
     */
    public void setpScale(Double pScale) {
        this.pScale = pScale;
    }

    /**
     * 图片格式
     */
    public String getpFormat() {
        return pFormat;
    }

    /**
     * 图片格式
     */
    public void setpFormat(String pFormat) {
        this.pFormat = pFormat;
    }

    /**
     * 创建用户 id
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 创建用户 id
     */
    public void setUserId(Long userid) {
        this.userId = userid;
    }

    /**
     * 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 编辑时间
     */
    public Date getEditTime() {
        return editTime;
    }

    /**
     * 编辑时间
     */
    public void setEditTime(Date editTime) {
        this.editTime = editTime;
    }

    /**
     * 更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 是否删除
     */
    public Integer getIsDelete() {
        return isDelete;
    }

    /**
     * 是否删除
     */
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Picture other = (Picture) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getpUrl() == null ? other.getpUrl() == null : this.getpUrl().equals(other.getpUrl()))
            && (this.getpName() == null ? other.getpName() == null : this.getpName().equals(other.getpName()))
            && (this.getpIntroduction() == null ? other.getpIntroduction() == null : this.getpIntroduction().equals(other.getpIntroduction()))
            && (this.getpCategory() == null ? other.getpCategory() == null : this.getpCategory().equals(other.getpCategory()))
            && (this.getpTags() == null ? other.getpTags() == null : this.getpTags().equals(other.getpTags()))
            && (this.getpSize() == null ? other.getpSize() == null : this.getpSize().equals(other.getpSize()))
            && (this.getpWidth() == null ? other.getpWidth() == null : this.getpWidth().equals(other.getpWidth()))
            && (this.getpHeight() == null ? other.getpHeight() == null : this.getpHeight().equals(other.getpHeight()))
            && (this.getpScale() == null ? other.getpScale() == null : this.getpScale().equals(other.getpScale()))
            && (this.getpFormat() == null ? other.getpFormat() == null : this.getpFormat().equals(other.getpFormat()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getEditTime() == null ? other.getEditTime() == null : this.getEditTime().equals(other.getEditTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getpUrl() == null) ? 0 : getpUrl().hashCode());
        result = prime * result + ((getpName() == null) ? 0 : getpName().hashCode());
        result = prime * result + ((getpIntroduction() == null) ? 0 : getpIntroduction().hashCode());
        result = prime * result + ((getpCategory() == null) ? 0 : getpCategory().hashCode());
        result = prime * result + ((getpTags() == null) ? 0 : getpTags().hashCode());
        result = prime * result + ((getpSize() == null) ? 0 : getpSize().hashCode());
        result = prime * result + ((getpWidth() == null) ? 0 : getpWidth().hashCode());
        result = prime * result + ((getpHeight() == null) ? 0 : getpHeight().hashCode());
        result = prime * result + ((getpScale() == null) ? 0 : getpScale().hashCode());
        result = prime * result + ((getpFormat() == null) ? 0 : getpFormat().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getEditTime() == null) ? 0 : getEditTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", pUrl=").append(pUrl);
        sb.append(", pName=").append(pName);
        sb.append(", pIntroduction=").append(pIntroduction);
        sb.append(", pCategory=").append(pCategory);
        sb.append(", pTags=").append(pTags);
        sb.append(", pSize=").append(pSize);
        sb.append(", pWidth=").append(pWidth);
        sb.append(", pHeight=").append(pHeight);
        sb.append(", pScale=").append(pScale);
        sb.append(", pFormat=").append(pFormat);
        sb.append(", userid=").append(userId);
        sb.append(", createTime=").append(createTime);
        sb.append(", editTime=").append(editTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", isDelete=").append(isDelete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}