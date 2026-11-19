package com.example.laptopshop.controller;
import com.example.laptopshop.entity.Product;
import com.example.laptopshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@Controller
public class CustomerController {
 private final ProductService productService;
 public CustomerController(ProductService productService){this.productService=productService;}

 @GetMapping("/")
 public String home(Model model){
  model.addAttribute("products",productService.getAll());
  return "index";
 }
 @GetMapping("/san-pham/{id}")
 public String detail(@PathVariable Long id, Model model){
  Product product=productService.getById(id).orElse(null);
  if(product==null) return "redirect:/";
  model.addAttribute("product",product); return "detail";
 }
 @GetMapping("/tim-kiem")
 public String search(@RequestParam(defaultValue="") String keyword, Model model){
  model.addAttribute("products",productService.search(keyword));
  model.addAttribute("keyword",keyword); return "index";
 }
 @PostMapping("/gio-hang/them/{id}")
 public String addCart(@PathVariable Long id, HttpSession session){
  if(productService.getById(id).isEmpty()) return "redirect:/";
  Map<Long,Integer> cart=(Map<Long,Integer>)session.getAttribute("cart");
  if(cart==null) cart=new LinkedHashMap<>();
  cart.merge(id,1,Integer::sum); session.setAttribute("cart",cart);
  return "redirect:/gio-hang";
 }
 @GetMapping("/gio-hang")
 public String cart(HttpSession session, Model model){
  Map<Long,Integer> cart=(Map<Long,Integer>)session.getAttribute("cart");
  if(cart==null) cart=new LinkedHashMap<>();
  List<Map<String,Object>> items=new ArrayList<>(); double total=0;
  for(var entry:cart.entrySet()){
   Product p=productService.getById(entry.getKey()).orElse(null);
   if(p!=null){double amount=p.getPrice().doubleValue()*entry.getValue(); total+=amount;
    Map<String,Object> row=new HashMap<>(); row.put("product",p); row.put("quantity",entry.getValue()); row.put("amount",amount); items.add(row);}
  }
  model.addAttribute("items",items); model.addAttribute("total",total); return "cart";
 }
 @PostMapping("/gio-hang/xoa/{id}")
 public String removeCart(@PathVariable Long id,HttpSession session){
  Map<Long,Integer> cart=(Map<Long,Integer>)session.getAttribute("cart");
  if(cart!=null) cart.remove(id); return "redirect:/gio-hang";
 }
 @PostMapping("/dat-hang")
 public String checkout(HttpSession session,Model model){
  Map<Long,Integer> cart=(Map<Long,Integer>)session.getAttribute("cart");
  if(cart==null||cart.isEmpty()){model.addAttribute("message","Giỏ hàng đang trống.");return "checkout";}
  model.addAttribute("message","Đặt hàng mẫu thành công! Vui lòng bổ sung lưu đơn hàng vào cơ sở dữ liệu theo yêu cầu nhóm.");
  session.removeAttribute("cart"); return "checkout";
 }
}
