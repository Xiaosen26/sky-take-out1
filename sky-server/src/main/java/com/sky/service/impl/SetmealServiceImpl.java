package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    DishMapper dishMapper;

    /**
     * 插入套餐信息和套餐中菜品信息
     * @param setmealDTO
     */
    public void addSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //插入套餐信息
        setmealMapper.addSetmeal(setmeal);
        //获取insert语句生成的菜品id
        Long setmealId=setmeal.getId();
        //设置套餐中的菜品信息
        List<SetmealDish> list=setmealDTO.getSetmealDishes();
        if(list!=null&&list.size()>0){
            list.forEach(setmealDish ->
                    setmealDish.setSetmealId(setmealId)
            );
            //插入套餐中菜品信息
            setmealDishMapper.addList(list);
        }


    }
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page=setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    public void delete(List<Long> ids) {
        //判断是否是起售套餐
        for (Long id:ids) {
            Setmeal setmeal=setmealMapper.getById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE){
                //正在起售不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //批量删除套餐
        setmealMapper.deleteByIds(ids);
        for (Long id:ids) {
            setmealDishMapper.deleteBySetmealId(id);
        }

    }

    /**
     * id查询套餐及其包含菜品
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {
        //查询套餐信息
        Setmeal setmeal=setmealMapper.getById(id);
        //查询套餐内菜品信息
        List<SetmealDish> setmealDishes=setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO=new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐基本信息
     * @param setmealDTO
     */
    public void updateSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        //修改套餐信息
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> list=setmealDTO.getSetmealDishes();
        if(list!=null&&list.size()>0){
            list.forEach(setmealDish ->
                    setmealDish.setSetmealId(setmealDTO.getId())
            );
            //插入套餐中菜品信息
            setmealDishMapper.addList(list);
        }

    }

    /**
     * 修改套餐售卖信息
     * @param status
     * @param id
     */
    public void updateDishStatus(Integer status, Long id) {
        Setmeal setmeal=new Setmeal();
        setmeal.setStatus(status);
        setmeal.setId(id);
        if(status==StatusConstant.ENABLE){
            List<SetmealDish> list = setmealDishMapper.getBySetmealId(id);
            for (SetmealDish setmealDish : list) {
                Dish dish=dishMapper.getById(setmealDish.getDishId());
                if(dish.getStatus()==StatusConstant.DISABLE){
                    //套餐中有未起售的商品
                    throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        setmealMapper.update(setmeal);
    }
    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
