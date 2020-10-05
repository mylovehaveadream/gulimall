package com.guigu.gulimall.product.web;

import com.guigu.gulimall.product.entity.CategoryEntity;
import com.guigu.gulimall.product.service.CategoryService;
import com.guigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        //TODO 1.查出所有一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();

        model.addAttribute("categorys",categoryEntities);   //放在页面的请求域中

        //视图解析器进行拼串,默认是转发
        //classpath:/templates + 返回值 + .html
        return "index";
    }

    //index/catalog.json
    @ResponseBody   //返回值以json的方式写出去
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        //1.获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redisson.getLock("my-lock");//只要名字相同就是同一把锁，随便写

        //2.加锁
        lock.lock(); //阻塞式等待，默认加的锁都是30秒时间
        //1.锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s,不用担心业务时间长，锁自动过期被删掉
        //2.加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30秒以后自动删除

        try {
            System.out.println("加锁成功，执行业务..."+Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //3.解锁,假设解锁代码没有运行，Redisson会不会出现死锁，不会，就算没有手动的解锁，他们也会为我们解锁
            System.out.println("释放锁..."+Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }


    /**
     * 车库停车
     * 3车位
     * 信号量也可以用作分布式限流
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        //park.acquire(); //获取一个信号，获取一个值，阻塞等待方法,占一个车位,
        boolean b = park.tryAcquire();//看有没有车位，有了就停，没了就算了，不会等待的，没有就返回false

        return "ok=>"+b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        park.release();//释放一个车位

        return "ok";
    }

    /**
     * 放假，锁门
     * 1班没人了
     * 5个班全部走完，我们可以锁大门
     * 使用分布式的闭锁
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);    //等待5个班的人都走完
        //5个班全部走完,才能执行业务放假
        door.await();   //等待，等待闭锁都完成

        return "放假了....";
    }


    @GetMapping("/gogo/{id}")
    @ResponseBody
    public String gogo(@PathVariable("id") Long id){
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();   //计数减一，走一个就减一

        return id+"班的人都走了....";
    }

}











