package com.offcn.cart.service;

import com.offcn.group.Cart;

import java.util.List;
//****************服务器端 编辑了内容***********************
public interface CartService {

    //添加商品到购物车

	//***************本地客户端小王进行修改**********************8

    /**
     *
     * @param cartList  当前购物车集合---篮子
     * @param itemId     添加到购物车的商品SKU编号
     * @param num        添加到购物车商品数量（？值范围 正值 也可使 负数）
     *   //返回值：新增商品后的购物车集合
     */
    public List<Cart>   addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num);




    /**
     * 从redis读取指定用户的购物车集合数据
     * @param username  用户名
     * @return  当前用户购物车集合
     */
    public List<Cart> findCartListFromRedis(String username);



    /**
     *  合并购物车方法
     * @param cookieCartlist
     * @param redisCartList
     * @return
     */
    public List<Cart> mergeCartList(List<Cart> cookieCartlist,List<Cart> redisCartList);




    /**
     * 写入指定用户购物车集合数据到redis
     * @param username
     * @param cartList
     */
    public void saveCartListToRedis(String username,List<Cart> cartList);


}
