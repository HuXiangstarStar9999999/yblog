package com.boot.controller;

import com.boot.pojo.*;
import com.boot.service.*;
import com.boot.utils.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Api("此类具体作用就是调试新开发的主题，不会在实际上线使用")
@RequestMapping(path = "/test")
@Controller
public class testController {


    @Value("${server.port}")
    private String port;

    @Autowired
    private com.boot.service.articleService articleService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private SpringSecurityUtil securityUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private com.boot.service.userDetailService userDetailService;

    @Autowired
    private com.boot.service.linkService linkService;


    @Autowired
    private com.boot.utils.browserOS browserOS;

    @Autowired
    private com.boot.service.visitorService visitorService;

    private final int DEFAULT_PAGENUM = 1;

    private final int COMMENT_PAGESIZE = 3;

    private final int type = 1;

    //new
    @Autowired
    private tagService tagService;

    @Autowired
    private cssUtil cssUtil;

    @Autowired
    private archiveService archiveService;


    //前10排行
    private static final List<Article> ArticleOrder_10(List<Article> articleList) {
        List<Article> list = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            list.add(articleList.get(i));
        }
        return list;
    }

    /**
     * 调试客户端首页
     */
    @RequestMapping(path = {"/"})
    public ModelAndView toIndex2(HttpSession session, HttpServletRequest request, @Value("访问首页") String desc) {
        ModelAndView modelAndView = new ModelAndView();
        //index2和index1不同之处
        List<tag> tags = tagService.selectTags_limit8();
        modelAndView.addObject("tags",tags);


        //


        PageHelper.startPage(1, 5);
        List<Article> list = articleService.selectAllArticle();
        PageInfo pageInfo = new PageInfo(list);
        List<Article> as = (List<Article>) redisTemplate.opsForValue().get("articleOrders10");
        if (as == null) {
            List<Article> articleOrders = ArticleOrder_10(articleService.selectAllArticleOrderByDesc());
            redisTemplate.opsForValue().set("articleOrders10", articleOrders, 60 * 1, TimeUnit.SECONDS);
            modelAndView.addObject("articleOrders", articleOrders);
        } else {
            modelAndView.addObject("articleOrders", as);
        }
//        System.out.println(pageInfo);
        //PageInfo{pageNum=1, pageSize=5, size=5, startRow=1, endRow=5, total=12, pages=3, list=Page{count=true, pageNum=1, pageSize=5, startRow=0, endRow=5, total=12, pages=3, reasonable=false, pageSizeZero=false}[Article{id=1, title='2018新版Java学习路线图', content='&ensp;&ensp;&ensp;&ensp;播妞深知广大爱好Java的人学习是多么困难，没视频没资源，上网花钱还老担心被骗。因此专门整理了新版的学习路线图，不管你是不懂电脑的小白，还是已经步入开发的大牛，这套路线路绝对不容错过！12年传智播客黑马程序员分享免费视频教程长达10余万小时，累计下载量3000余万次，受益人数达千万。2018年我们不忘初心，继续前行。 路线图的宗旨就是分享，专业，便利，让喜爱Java的人，都能平等的学习。从今天起不要再找借口，不要再说想学Java却没有资源，赶快行动起来，Java等你来探索，高薪距你只差一步！

        /**
         * xxx个人博客标题
         */
        SecurityContextImpl securityContext = (SecurityContextImpl) session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext != null) {
            String name = securityUtil.currentUser(session);
            if (name != null && !name.equals("")) {
                userDetail userDetail = userDetailService.selectUserDetailByUserName(name);
                modelAndView.addObject("userDetail", userDetail);
            }
        } else {
            userDetail userDetail = null;
            modelAndView.addObject("userDetail", userDetail);
        }


        //友链
        List<link> links = linkService.selectAllLink();
        modelAndView.addObject("links", links);

        modelAndView.addObject("articles", list);
        modelAndView.addObject("commons", Commons.getInstance());
        modelAndView.addObject("pageInfo", pageInfo);

        modelAndView.setViewName("client/index2");
        return modelAndView;
    }

    @GetMapping(path = "/archive")
    @ApiOperation(value = "去归档页面")
    public ModelAndView toArchiveList(HttpSession session, HttpServletRequest request, @Value("归档页面") String desc){
        ModelAndView modelAndView = new ModelAndView();

        //不同
        List<tag> tags = tagService.selectTags_limit8();
        modelAndView.addObject("tags",tags);



        //前端进行判断，isArchive是不是等于空，如果不是就是归档页面，进行页面代码的复用，省去写一个新的页面
        modelAndView.addObject("isArchive","true");

        List<archive> archives = archiveService.selectAllArchiveGroup(); //获取归档分组信息
//        modelAndView.addObject("archives",archives);
        modelAndView.addObject("cssUtil",cssUtil);

        //添加访客信息
        visitor visitor = visitorUtil.getVisitor(request, desc);
        String key1 = "visit_ip_" + visitor.getVisit_ip() + "_type_" + type;
        String s = (String) redisTemplate.opsForValue().get(key1);
        if (StringUtils.isEmpty(s)) {
            visitorService.insertVisitor(visitor);
            //由ip和type组成的key放入redis缓存,5分钟内访问过的不再添加访客
            redisTemplate.opsForValue().set(key1, "1", 60 * 5, TimeUnit.SECONDS);
        }


        /**
         * 可以通过hashMap来维护归档数据
         * key=年月分组
         * value=List集合，存储对应的分组出来的数据
         */
        Map<String, List<Article>> concurrentHashMap = new ConcurrentHashMap<>();
        for (archive archive : archives) {
            String months = archive.getMonths();
            List<Article> articles = archiveService.selectArticleByarchiveTime(months);
            concurrentHashMap.put(months,articles);
        }
        modelAndView.addObject("archiveMap",concurrentHashMap); //传入前端


        List<Article> as = (List<Article>) redisTemplate.opsForValue().get("articleOrders10");
        if (as == null) {
            List<Article> articleOrders = ArticleOrder_10(articleService.selectAllArticleOrderByDesc());
            redisTemplate.opsForValue().set("articleOrders10", articleOrders, 60 * 1, TimeUnit.SECONDS);
            modelAndView.addObject("articleOrders", articleOrders);
        } else {
            modelAndView.addObject("articleOrders", as);
        }

        /**
         * xxx个人博客标题
         */
        SecurityContextImpl securityContext = (SecurityContextImpl) session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext != null) {
            String name = securityUtil.currentUser(session);
            if (name != null && !name.equals("")) {
                userDetail userDetail = userDetailService.selectUserDetailByUserName(name);
                modelAndView.addObject("userDetail", userDetail);
            }
        } else {
            userDetail userDetail = null;
            modelAndView.addObject("userDetail", userDetail);
        }


        //友链
        List<link> links = linkService.selectAllLink();
        modelAndView.addObject("links",links);
        modelAndView.addObject("commons", Commons.getInstance());
        modelAndView.setViewName("client/index2");
        return modelAndView;
    }

    @GetMapping(path = "/article/{articleId}")
    public ModelAndView toArticleDetailByID(@PathVariable("articleId") Integer articleId, HttpServletRequest request, HttpSession session, @Value("访问文章") String desc) {

        ModelAndView modelAndView = new ModelAndView();
        //index2和index1不同之处
        List<tag> tags = tagService.selectTags_limit8();
        modelAndView.addObject("tags",tags);
        List<Article> as = (List<Article>) redisTemplate.opsForValue().get("articleOrders10");
        if (as == null) {
            List<Article> articleOrders = ArticleOrder_10(articleService.selectAllArticleOrderByDesc());
            redisTemplate.opsForValue().set("articleOrders10", articleOrders, 60 * 1, TimeUnit.SECONDS);
            modelAndView.addObject("articleOrders", articleOrders);
        } else {
            modelAndView.addObject("articleOrders", as);
        }





        //当某个ip在短时间内不断访问某篇文章会造成点击量+1
        //需求：我们想让用户ip访问文章后2分钟内，再次点击这篇文章点击量不会+1，防止用户刷点击量
        String ipAddr = ipUtils.getIpAddr(request); //1.先获取ip
        String key = "ip_" + ipAddr + "_ar_" + articleId;
        Object o = redisTemplate.opsForValue().get(key);//2.通过ip和文章id，去查询有没有对应的值
        if (o == null) {
            //文章点击数+1
            articleService.updateHits(articleId);
            redisTemplate.opsForValue().set(key, "1", 60 * 2, TimeUnit.SECONDS); //设置2分钟的过期时间
        }


        //添加访客信息
        visitor visitor = visitorUtil.getVisitor(request, desc);
        String key1 = "visit_ip_" + visitor.getVisit_ip() + "_type_" + type;
        String s = (String) redisTemplate.opsForValue().get(key1);
        if (StringUtils.isEmpty(s)) {
            visitorService.insertVisitor(visitor);
            //由ip和type组成的key放入redis缓存,5分钟内访问过的不再添加访客
            redisTemplate.opsForValue().set(key1, "1", 60 * 5, TimeUnit.SECONDS);
        }


        boolean res = false; //判断是否传入参数“c”
        Boolean hasComment = commentService.hasComment(articleId);
        Article article = null;


        /**
         * xxx个人博客标题
         */
        SecurityContextImpl securityContext = (SecurityContextImpl) session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext != null) {
            String name = securityUtil.currentUser(session);
            if (name != null && !name.equals("")) {
                userDetail userDetail = userDetailService.selectUserDetailByUserName(name);
                modelAndView.addObject("name",name);
                modelAndView.addObject("userDetail", userDetail);
            }
        } else {
            userDetail userDetail = null;
            modelAndView.addObject("userDetail", userDetail);
        }


        //从Redis数据库中获取指定文章id的数据，如果没有就从数据库查询，然后在放入redis中
        article = (Article) redisTemplate.opsForValue().get("articleId_" + articleId);
        if (article == null) {
            article = articleService.selectArticleByArticleIdNoComment(articleId);//查文章内容（没有评论）
            redisTemplate.opsForValue().set("articleId_" + articleId, article);
        }
        String c = request.getParameter("c");
        int pageNum = 0;
        if (c == null || c.equals("")) {
            res = false;
        } else {
            try {
                pageNum = Integer.parseInt(c);
                res = true;
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                res = false;
            }
        }
        if (res) {
            if (hasComment) {
                PageHelper.startPage(pageNum, COMMENT_PAGESIZE);
                List<Comment> comments = commentService.CommentByArticleId(articleId); //一定是要mybatis查询才行，get方法获取不行
                PageInfo pageInfo = new PageInfo(comments);
                modelAndView.addObject("pageInfo", pageInfo);
                modelAndView.addObject("comments", comments);
            }
            modelAndView.addObject("article", article);
            modelAndView.addObject("commons", Commons.getInstance());
            modelAndView.addObject("articleId", articleId);
            modelAndView.setViewName("client/articleDetails2");
            return modelAndView;


        } else {
            if (hasComment) {
//                List<Comment> comments = (List<Comment>) redisTemplate.opsForValue().get("cm_aid_" + articleId + "_n_" + pageNum + "_s_" + COMMENT_PAGESIZE);
//                if(comments==null){
//                    PageHelper.startPage(DEFAULT_PAGENUM,COMMENT_PAGESIZE);
//                     comments = commentService.CommentByArticleId(articleId); //一定是要mybatis查询才行，get方法获取不行
//                    redisTemplate.opsForValue().set("cm_aid_" + articleId + "_n_" + DEFAULT_PAGENUM + "_s_" + COMMENT_PAGESIZE,comments);
//                }
                PageHelper.startPage(DEFAULT_PAGENUM, COMMENT_PAGESIZE);
                List<Comment> comments = commentService.CommentByArticleId(articleId); //一定是要mybatis查询才行，get方法获取不行
                PageInfo pageInfo = new PageInfo(comments);
                modelAndView.addObject("pageInfo", pageInfo);
                modelAndView.addObject("comments", comments);
            }

            //友链
            List<link> links = linkService.selectAllLink();
            modelAndView.addObject("links", links);

            modelAndView.addObject("article", article);
            modelAndView.addObject("commons", Commons.getInstance());
            modelAndView.addObject("articleId", articleId);
            modelAndView.setViewName("client/articleDetails2");
            return modelAndView;

        }
    }


}