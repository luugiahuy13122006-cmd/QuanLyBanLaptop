
package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Categories")
public class Category {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 @Column(name = "CategoryID")
 private Integer id;

 @Column(name = "CategoryName", nullable = false, unique = true, length = 100)
 private String name;

 @Column(name = "Description", length = 500)
 private String description;

 @Column(name = "IsActive", nullable = false)
 private Boolean isActive = true;

 @Column(name = "CreatedAt", insertable = false, updatable = false)
 private LocalDateTime createdAt;

 public Category() {
 }

 public Integer getId() {
  return id;
 }

 public void setId(Integer id) {
  this.id = id;
 }

 public String getName() {
  return name;
 }

 public void setName(String name) {
  this.name = name;
 }

 public String getDescription() {
  return description;
 }

 public void setDescription(String description) {
  this.description = description;
 }

 public Boolean getIsActive() {
  return isActive;
 }

 public void setIsActive(Boolean isActive) {
  this.isActive = isActive;
 }

 public LocalDateTime getCreatedAt() {
  return createdAt;
 }
}
