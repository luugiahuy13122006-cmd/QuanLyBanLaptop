
package com.laptopshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Brands")
public class Brand {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 @Column(name = "BrandID")
 private Integer id;

 @Column(name = "BrandName", nullable = false, unique = true, length = 100)
 private String name;

 @Column(name = "Country", length = 100)
 private String country;

 @Column(name = "Description", length = 500)
 private String description;

 @Column(name = "IsActive", nullable = false)
 private Boolean isActive = true;

 public Brand() {
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

 public String getCountry() {
  return country;
 }

 public void setCountry(String country) {
  this.country = country;
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
}
