package com.guigu.gulimall.product.web;

import com.guigu.gulimall.product.entity.CategoryEntity;
import com.guigu.gulimall.product.service.CategoryService;
import com.guigu.gulimall.product.vo.Catelog2Vo;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate redisTemplate;

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
        //lock.lock(); //阻塞式等待，默认加的锁都是30秒时间
        //1.锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s,不用担心业务时间长，锁自动过期被删掉
        //2.加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30秒以后自动删除

        //问题：lock.lock(10, TimeUnit.SECONDS);在锁时间到了以后，不会自动续期
        //1.如果我们传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是我们指定的时间
        //2.如果我们未指定锁的超时时间，就使用30 * 1000【LockWatchdogTimeout看门狗的默认时间】
        //    只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】，每隔10s都会自动再次续期，续成30s
        //    internalLockLeaseTime[看门狗时间] / 3，过了10s以后进行续期
        lock.lock(10, TimeUnit.SECONDS);  //10秒自动解锁，自动解锁时间一定要大于业务的执行时间，因为它不会自动的续期

        //最佳实战，使用它
        //1.lock.lock(10, TimeUnit.SECONDS);省掉了整个续期操作，手动解锁

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


    //保证一定能读到最新的数据，修改期间，写锁是一个排他锁(互斥锁、独享锁)，读锁是一个共享锁
    //写锁没释放读就必须等待
    //读 + 读：相当于无锁，并发读，只会在redis中记录好，所有当前的读锁。他们都会同时加锁成功
    //写 + 读：等待写锁释放
    //写 + 写：阻塞方式
    //读 + 写：有读锁，写也需要等待
    //只要有写的存在，都必须等待
    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){ //写

        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.writeLock();
        try {
            //1.改数据加写锁，读数据加读锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue",s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }

        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){ //读
        //同一把读写锁
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        //加读锁
        //在进行写锁的时候，读锁就需要进行等待，等写锁进行释放
        RLock rLock = lock.readLock();
        String s = "";
        rLock.lock();
        try {
            s = redisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }

        return s;
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











