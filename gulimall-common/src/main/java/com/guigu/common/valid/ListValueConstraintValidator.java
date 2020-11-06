package com.guigu.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 *  @ListValue(vals={0,1})
 * 	private Integer showStatus;
 * 	ConstraintValidator<校验的注解,标注在哪个类型上的>
 */
public class ListValueConstraintValidator implements ConstraintValidator<ListValue,Integer> {

    private Set<Integer> set = new HashSet();

    //初始化方法
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        for(int val:vals){
            set.add(val);
        }
    }

    //判断是否校验成功
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        //integer是提交过来，需要校验的值
        //constraintValidatorContext：整个校验的上下文环境信息

        return set.contains(integer);
    }
}
