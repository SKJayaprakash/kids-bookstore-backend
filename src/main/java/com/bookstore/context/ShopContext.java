package com.bookstore.context;

import com.bookstore.entity.Shop;

public class ShopContext {

    private static final ThreadLocal<Shop> currentShop = new ThreadLocal<>();

    public static void setCurrentShop(Shop shop) {
        currentShop.set(shop);
    }

    public static Shop getCurrentShop() {
        return currentShop.get();
    }

    public static void clear() {
        currentShop.remove();
    }
}
