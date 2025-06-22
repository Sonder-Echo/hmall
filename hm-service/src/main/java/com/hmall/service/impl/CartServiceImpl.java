package com.hmall.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.protobuf.ServiceException;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.exception.CommonException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.domain.dto.CartFormDTO;
import com.hmall.domain.dto.ItemDTO;
import com.hmall.domain.po.Cart;
import com.hmall.domain.po.Item;
import com.hmall.domain.vo.CartVO;
import com.hmall.mapper.CartMapper;
import com.hmall.service.ICartService;
import com.hmall.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    private final IItemService itemService;
    private final CartMapper cartMapper;

    @Override
    public void addItem2Cart(CartFormDTO cartFormDTO) {
        if(checkItemExist(cartFormDTO.getItemId(), UserContext.getUser())){
            // 商品已存在
            lambdaUpdate().setSql("num = num + 1")
                    .eq(Cart::getItemId, cartFormDTO.getItemId())
                    .eq(Cart::getUserId, UserContext.getUser())
                    .update();
        }else {
            // 商品不存在
            //如果该用户购物车的商品没有超过10种，则新增一条购物车记录
            Long count = lambdaQuery().eq(Cart::getUserId, UserContext.getUser()).count();
            if(count >= 10){
                throw new BizIllegalException("购物车商品数量已超过限制！");
            }
            Cart cart = BeanUtil.copyProperties(cartFormDTO, Cart.class);
            cart.setUserId(UserContext.getUser());
            save(cart);
        }
    }

    /**
     * 检查用户购物车是否已存在该商品
     * @param itemId
     * @param user
     * @return
     */
    private boolean checkItemExist(Long itemId, Long user) {
        Long count = lambdaQuery()
                .eq(Cart::getUserId, user)
                .eq(Cart::getItemId, itemId)
                .count();
        return count > 0;
    }

    @Override
    public List<CartVO> queryMyCarts() {
        //1.查询当前用户购物车列表
        List<Cart> carts = lambdaQuery().eq(Cart::getUserId, UserContext.getUser()).list();
        if(!CollUtils.isEmpty(carts)){
            //2.根据商品id查询商品最新价格、状态、库存等信息
            List<CartVO> cartVOS = BeanUtil.copyToList(carts, CartVO.class);
            //2.1收集商品id
            List<Long> itemIdList = cartVOS.stream().map(CartVO::getItemId).collect(Collectors.toList());
            //2.2根据商品id集合批量查询商品
            Map<Long, Item> itemMap = itemService.listByIds(itemIdList).stream().collect(Collectors.toMap(Item::getId, Function.identity()));

            //3.遍历每个购物车商品，设置商品属性
            cartVOS.forEach(cartVO -> {
                Item item = itemMap.get(cartVO.getItemId());
                cartVO.setNewPrice(item.getPrice());
                cartVO.setStatus(item.getStatus());
                cartVO.setStock(item.getStock());
            });
            return cartVOS;
        }

        return CollUtils.emptyList();
    }

    @Override
    public void removeByItemIds(Collection<Long> itemIds) {
        // 1.构建删除条件，userId和itemId
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<Cart>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, UserContext.getUser())
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(queryWrapper);
    }
}
