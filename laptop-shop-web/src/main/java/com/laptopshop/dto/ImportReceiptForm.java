package com.laptopshop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class ImportReceiptForm {

    @NotBlank(message = "Vui lòng nhập nhà cung cấp")
    @Size(max = 150, message = "Nhà cung cấp tối đa 150 ký tự")
    private String supplier;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    @NotEmpty(message = "Phiếu nhập phải có ít nhất 1 laptop")
    @Valid
    private List<ImportItemRequest> items = new ArrayList<>();

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<ImportItemRequest> getItems() { return items; }
    public void setItems(List<ImportItemRequest> items) { this.items = items; }
}
