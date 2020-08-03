package com.offcn.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.cart.service.CartService;
import com.offcn.group.Cart;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Service
public class CartServiceImpl implements CartService {

    //ע��sku�����ݲ����ӿ�
    @Autowired
    private TbItemMapper itemMapper;

    //ע��Redisģ�����������
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
       //1������sku��Ʒ��ţ���ѯsku��Ʒ��Ϣ
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        //1.1���ж�sku�����Ƿ�Ϊ��
        if(item==null){
            //��Ʒ�����ڣ�������ӹ��ﳵ�������׳��쳣
            throw new RuntimeException("Ҫ��ӵ����ﳵ����Ʒ������");
        }
        //1.2���ж���Ʒ״̬ �Ƿ������ͨ�� 1
        if(!item.getStatus().equals("1")){
            throw new RuntimeException("��Ʒδ���ͨ��");
        }

        //2����ȡ����Ʒ�����̼ұ��
        String sellerId = item.getSellerId();
        //3�������̼ұ�ţ�ȥ�������ﳵ���ϣ��ж��Ƿ���ڸ��̼ҹ��ﳵ����
        Cart cart = searchCartListBySellerId(cartList, sellerId);
        //4���жϸ��̼ҵĹ��ﳵ�����Ƿ����
        if(cart==null){
            //5����һ����������̼ҹ��ﳵ���󲻴��ڣ��������̼ҵĹ��ﳵ����
            cart=new Cart();
            //���ù��ﳵ���������̼ұ��
            cart.setSellerId(sellerId);
            //���ù��ﳵ�����̼�����
            cart.setSellerName(item.getSeller());
            //����һ��������ϸ����
            List<TbOrderItem> orderItemList=new ArrayList<>();
            //����������ϸ
            TbOrderItem orderItem = createOrderItem(item, num);
            //�ѹ�����ϸ��ŵ�������ϸ����
            orderItemList.add(orderItem);
            //�ѹ�����ϸ������ӵ����ﳵ����
            cart.setOrderItemList(orderItemList);

            //ǧ������ǣ����½��Ĺ��ﳵ������ӵ���ǰ���ﳵ����
            cartList.add(cart);


        }else {
            //6���ڶ�����������ﳵ���ϴ��ڸ��̼ҹ��ﳵ����

            //7����Ҫ�ж�Ҫ���뵽���ﳵ��Ʒ�ڸ��̼ҹ��ﳵ����Ĺ�����ϸ���Ƿ����
            TbOrderItem orderItem = searchOrderItemByItemid(cart.getOrderItemList(), itemId);
             //8����һ�������Ҫ��ӵ����ﳵ��Ʒ�ڹ�����ϸ��Ŀǰ������
            if(orderItem==null){
                //����һ��������ϸ����
                orderItem= createOrderItem(item,num);
                //�ѹ�����ϸ���ӵ�ǰ�������Ĺ�����ϸ����
                cart.getOrderItemList().add(orderItem);
            }else {
                //9���ڶ��������Ҫ��ӵ���Ʒ�ڹ��ﳵ����Ĺ�����ϸ���Ѿ�����
                //���¹������� ԭ�й�������+�µĹ�������
                orderItem.setNum(orderItem.getNum()+num);
                //���µ�ǰ��Ʒ�ϼƽ��
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum()*item.getPrice().doubleValue()));

                //�������ﳵ����������ɺ���Ҫ�жϵ�ǰ���������Ƿ�С��1�������ǰ��Ʒ��������С��1��ҪҪ��ǰ���ﳵ
                //������ϸ�����Ƴ�����Ʒ
                if(orderItem.getNum()<1){
                    cart.getOrderItemList().remove(orderItem);
                }

                //�����ﳵ�Ĺ�����ϸ����Ϊ0���ӹ��ﳵ�����Ƴ���ǰ�̼ҹ��ﳵ����
                if(cart.getOrderItemList().size()==0){
                    cartList.remove(cart);
                }
            }

        }

        return cartList;
    }



    /**
     * �������ﳵ���ϣ��ж��Ƿ���ڸ��̼ҹ��ﳵ����
     * @param cartList  Ҫ�����Ĺ��ﳵ����
     * @param sellerId   Ҫ�ȶԵ��̼ұ��
     * @return   �������ĸ��̼ҵĹ��ﳵ����
     */
    private Cart searchCartListBySellerId(List<Cart> cartList,String sellerId){

        //�жϹ��ﳵ�����Ƿ�Ϊ��
        if(cartList!=null){
            //�������ﳵ����
            for (Cart cart : cartList) {
                //��ȡ���ﳵ������̼����ԣ��ʹ�����̼ұ�Ž��бȶ�
                if(cart.getSellerId().equals(sellerId)){
                    //���ص�ǰ���ﳵ����
                    return cart;
                }
            }
        }

        return null;

    }



    /**
     * ����������ϸ
     * @param item  Ҫ�������Ʒsku����
     * @param num   Ҫ���������
     * @return   ������ϸ����
     */
    private TbOrderItem createOrderItem(TbItem item,Integer num){

        //�жϹ��������Ƿ�С��1�������������Ϸ�

        if(num<1){
            throw new RuntimeException("�����������Ϸ�");
        }
        //new������ϸ����
        TbOrderItem orderItem = new TbOrderItem();

        //���ù�����ϸ����
        //������Ʒ���
        orderItem.setGoodsId(item.getGoodsId());
        //����sku���
        orderItem.setItemId(item.getId());
        //���ù�������
        orderItem.setNum(num);
        //ͼƬ
        orderItem.setPicPath(item.getImage());
        //�۸�
        orderItem.setPrice(item.getPrice());
        //�̼ұ��
        orderItem.setSellerId(item.getSellerId());
        //��Ʒ����
        orderItem.setTitle(item.getTitle());
        //���õ�����Ʒ�ϼƷ���  = ��������*��Ʒ����
        //�������������߾���
        BigDecimal bigDecimalNum = new BigDecimal(num);

        orderItem.setTotalFee(bigDecimalNum.multiply(item.getPrice()));

        return orderItem;
    }



    /**
     * //�ж�ָ���̼ҹ��ﳵ�����Ƿ����ָ��������ϸ
     * @param orderItemList
     * @param itemId
     * @return
     */
    private TbOrderItem searchOrderItemByItemid(List<TbOrderItem> orderItemList,Long itemId){
        //����������ϸ
        for (TbOrderItem orderItem : orderItemList) {
            //��ȡ������ϸ������Ʒsku��ţ��ʹ��ݹ�������Ʒsku��űȶԣ����Ƿ�һ��
            if(orderItem.getItemId().longValue()==itemId.longValue()){
                return orderItem;
            }
        }

        return null;
    }

    @Override
    public List<Cart> findCartListFromRedis(String username) {

        List<Cart> cartList=null;
        cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);


        if(cartList==null){
          cartList=  new ArrayList<>();
        }

        return cartList;

    }

    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        System.out.println("���빺�ﳵ����:"+username);
        redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    @Override
    public List<Cart> mergeCartList(List<Cart> cookieCartlist, List<Cart> redisCartList) {
        //����cookie���ﳵ����
        for (Cart cart : cookieCartlist) {
            //ѭ������������ϸ����
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                //������ӵ����ﳵ����
              redisCartList=  addGoodsToCartList(redisCartList,orderItem.getItemId(),orderItem.getNum());
            }
        }

        return redisCartList;
    }
}
