package com.laptopshop.controller;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
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
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        String sortField = switch (sortBy) {
            case "customerName", "phone", "email" -> sortBy;
            default -> "id";
        };
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Page<Customer> customerPage = customerService.getCustomers(keyword, PageRequest.of(page, size, sort));
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "admin/customers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        return "admin/customers/form";
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
        return "admin/customers/form";
    }

    @PostMapping("/save")
    public String saveCustomer(
            @Valid @ModelAttribute("customerDTO") CustomerDTO customerDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/customers/form";
        }
        try {
            customerService.saveCustomer(customerDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin khách hàng thành công!");
            return "redirect:/admin/customers";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/customers/form";
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
