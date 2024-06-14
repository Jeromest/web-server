### 使用kotlin写一个简单的web服务器

#### 程序定义了两个路由，分别为`/`、`/hello`以及错误页面

#### 访问`/`时，返回200状态码
![img_2.png](img_2.png)

#### 访问`/hello`时，返回200状态码和`Hello World!`
![img.png](img.png)

#### 访问不存在的路径时，返回404状态码和`404 Not Found`
![img_1.png](img_1.png)

#### 更多关于http消息的介绍，请参考[HTTP消息](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Messages)