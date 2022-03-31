package com.app.api;

import com.app.persistence.model.Category;
import com.app.service.OrdersService;

import java.util.List;
import java.util.stream.IntStream;

public class App {

    public static void main(String[] args) {

        String basePath = "C:\\Users\\jambop\\Desktop\\szkolenie\\projects\\PROJECT_03_ZAKUPY\\persistence\\src\\main\\resources\\";

        List<String> filenames = IntStream.range(1, 3)
                .boxed()
                .map(number -> "%scustomers%d.json".formatted(basePath, number))
                .toList();

        OrdersService ordersService = new OrdersService(filenames);
        System.out.println(ordersService.getCustomerWithMaxExpense());
        System.out.println(ordersService.getCustomerWithMaxExpenseOnCategory(Category.ODZIEZ));
        System.out.println(ordersService.getCustomerWithMaxExpenseOnCategory(Category.KSIAZKA));
        System.out.println(ordersService.getCustomersAndDebts());
        System.out.println(ordersService.getCategoriesWithAveragePrices());
        System.out.println(ordersService.getCategoriesAndProductsWithMaxPrice());
        System.out.println(ordersService.getCategoriesAndProductsWithMinPrice());
        System.out.println(ordersService.getAgesWithPopularCategories());
        System.out.println(ordersService.getCategoriesAndCustomers());
    }

}
