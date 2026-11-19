package com.laptopshop.controller;

import com.laptopshop.dto.CustomerDTO;
import com.laptopshop.entity.Customer;
import com.laptopshop.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String listCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        Page<Customer> customerPage = customerService.getCustomers(
                keyword, PageRequest.of(page, size, Sort.by("id").descending()));

        model.addAttribute("customerPage", customerPage);
        model.addAttribute("keyword", keyword);
        return "customers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        return "customers/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Customer customer = customerService.getCustomerById(id);
        CustomerDTO dto = new CustomerDTO(
                customer.getId(),
                customer.getCustomerName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getAccountId()
        );
        model.addAttribute("customerDTO", dto);
        return "customers/form";
    }

    @PostMapping("/save")
    public String saveCustomer(
            @Valid @ModelAttribute("customerDTO") CustomerDTO customerDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "customers/form";
        }
        try {
            customerService.saveCustomer(customerDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin khách hàng thành công!");
            return "redirect:/admin/customers";
        } catch (RuntimeException ex) {
            bindingResult.rejectValue("phone", "error.customerDTO", ex.getMessage());
            return "customers/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khách hàng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/customers";
    }
}
