package com.offcn.cart.service;

import com.offcn.group.Cart;

import java.util.List;

public interface CartService {

    //�����Ʒ�����ﳵ

    /**
     *
     * @param cartList  ��ǰ���ﳵ����---����
     * @param itemId     ��ӵ����ﳵ����ƷSKU���
     * @param num        ��ӵ����ﳵ��Ʒ��������ֵ��Χ ��ֵ Ҳ��ʹ ������
     *   //����ֵ��������Ʒ��Ĺ��ﳵ����
     */
    public List<Cart>   addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num);




    /**
     * ��redis��ȡָ���û��Ĺ��ﳵ��������
     * @param username  �û���
     * @return  ��ǰ�û����ﳵ����
     */
    public List<Cart> findCartListFromRedis(String username);



    /**
     *  �ϲ����ﳵ����
     * @param cookieCartlist
     * @param redisCartList
     * @return
     */
    public List<Cart> mergeCartList(List<Cart> cookieCartlist,List<Cart> redisCartList);




    /**
     * д��ָ���û����ﳵ�������ݵ�redis
     * @param username
     * @param cartList
     */
    public void saveCartListToRedis(String username,List<Cart> cartList);


}
