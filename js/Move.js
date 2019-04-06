/* 
* @param
*     obj -- object 节点对象
*     myJson -- object 属性与目标值json格式的键值对
*     time -- number 运动需要的时间 单位: 毫秒 默认值: 1000毫秒
*     tw -- string 运动效果 可选择的值: Quad, Cubic, Quart, Quint, Sine, Expo, Circ, Elastic, Back, Bounce 默认值: Quad
*     curve -- string 运动曲线 可选择的值: easeIn, easeOut, easeInOut 默认值: easeInOut
*     cb -- object 回调函数
* @return 
*     undefined
*/
(function (){
    
    window.requestAnimationFrame = window.requestAnimationFrame || function (obj) {return setTimeout(obj, 1000/60);};
    window.cancelAnimationFrame = window.cancelAnimationFrame || clearTimeout;
    var Tween = {
        Quad: {
            easeIn: function(t, b, c, d) {
                return c * (t /= d) * t + b;
            },
            easeOut: function(t, b, c, d) {
                return -c *(t /= d)*(t-2) + b;
            },
            easeInOut: function(t, b, c, d) {
                if ((t /= d / 2) < 1) return c / 2 * t * t + b;
                return -c / 2 * ((--t) * (t-2) - 1) + b;
            }
        },
        Cubic: {
            easeIn: function(t, b, c, d) {
                return c * (t /= d) * t * t + b;
            },
            easeOut: function(t, b, c, d) {
                return c * ((t = t/d - 1) * t * t + 1) + b;
            },
            easeInOut: function(t, b, c, d) {
                if ((t /= d / 2) < 1) return c / 2 * t * t*t + b;
                return c / 2*((t -= 2) * t * t + 2) + b;
            }
        },
        Quart: {
            easeIn: function(t, b, c, d) {
                return c * (t /= d) * t * t*t + b;
            },
            easeOut: function(t, b, c, d) {
                return -c * ((t = t/d - 1) * t * t*t - 1) + b;
            },
            easeInOut: function(t, b, c, d) {
                if ((t /= d / 2) < 1) return c / 2 * t * t * t * t + b;
                return -c / 2 * ((t -= 2) * t * t*t - 2) + b;
            }
        },
        Quint: {
            easeIn: function(t, b, c, d) {
                return c * (t /= d) * t * t * t * t + b;
            },
            easeOut: function(t, b, c, d) {
                return c * ((t = t/d - 1) * t * t * t * t + 1) + b;
            },
            easeInOut: function(t, b, c, d) {
                if ((t /= d / 2) < 1) return c / 2 * t * t * t * t * t + b;
                return c / 2*((t -= 2) * t * t * t * t + 2) + b;
            }
        },
        Sine: {
            easeIn: function(t, b, c, d) {
                return -c * Math.cos(t/d * (Math.PI/2)) + c + b;
            },
            easeOut: function(t, b, c, d) {
                return c * Math.sin(t/d * (Math.PI/2)) + b;
            },
            easeInOut: function(t, b, c, d) {
                return -c / 2 * (Math.cos(Math.PI * t/d) - 1) + b;
            }
        },
        Expo: {
            easeIn: function(t, b, c, d) {
                return (t==0) ? b : c * Math.pow(2, 10 * (t/d - 1)) + b;
            },
            easeOut: function(t, b, c, d) {
                return (t==d) ? b + c : c * (-Math.pow(2, -10 * t/d) + 1) + b;
            },
            easeInOut: function(t, b, c, d) {
                if (t==0) return b;
                if (t==d) return b+c;
                if ((t /= d / 2) < 1) return c / 2 * Math.pow(2, 10 * (t - 1)) + b;
                return c / 2 * (-Math.pow(2, -10 * --t) + 2) + b;
            }
        },
        Circ: {
            easeIn: function(t, b, c, d) {
                return -c * (Math.sqrt(1 - (t /= d) * t) - 1) + b;
            },
            easeOut: function(t, b, c, d) {
                return c * Math.sqrt(1 - (t = t/d - 1) * t) + b;
            },
            easeInOut: function(t, b, c, d) {
                if ((t /= d / 2) < 1) return -c / 2 * (Math.sqrt(1 - t * t) - 1) + b;
                return c / 2 * (Math.sqrt(1 - (t -= 2) * t) + 1) + b;
            }
        },
        Elastic: {
            easeIn: function(t, b, c, d, a, p) {
                var s;
                if (t==0) return b;
                if ((t /= d) == 1) return b + c;
                if (typeof p == "undefined") p = d * .3;
                if (!a || a < Math.abs(c)) {
                    s = p / 4;
                    a = c;
                } else {
                    s = p / (2 * Math.PI) * Math.asin(c / a);
                }
                return -(a * Math.pow(2, 10 * (t -= 1)) * Math.sin((t * d - s) * (2 * Math.PI) / p)) + b;
            },
            easeOut: function(t, b, c, d, a, p) {
                var s;
                if (t==0) return b;
                if ((t /= d) == 1) return b + c;
                if (typeof p == "undefined") p = d * .3;
                if (!a || a < Math.abs(c)) {
                    a = c; 
                    s = p / 4;
                } else {
                    s = p/(2*Math.PI) * Math.asin(c/a);
                }
                return (a * Math.pow(2, -10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p) + c + b);
            },
            easeInOut: function(t, b, c, d, a, p) {
                var s;
                if (t==0) return b;
                if ((t /= d / 2) == 2) return b+c;
                if (typeof p == "undefined") p = d * (.3 * 1.5);
                if (!a || a < Math.abs(c)) {
                    a = c; 
                    s = p / 4;
                } else {
                    s = p / (2  *Math.PI) * Math.asin(c / a);
                }
                if (t < 1) return -.5 * (a * Math.pow(2, 10* (t -=1 )) * Math.sin((t * d - s) * (2 * Math.PI) / p)) + b;
                return a * Math.pow(2, -10 * (t -= 1)) * Math.sin((t * d - s) * (2 * Math.PI) / p ) * .5 + c + b;
            }
        },
        Back: {
            easeIn: function(t, b, c, d, s) {
                if (typeof s == "undefined") s = 1.70158;
                return c * (t /= d) * t * ((s + 1) * t - s) + b;
            },
            easeOut: function(t, b, c, d, s) {
                if (typeof s == "undefined") s = 1.70158;
                return c * ((t = t/d - 1) * t * ((s + 1) * t + s) + 1) + b;
            },
            easeInOut: function(t, b, c, d, s) {
                if (typeof s == "undefined") s = 1.70158; 
                if ((t /= d / 2) < 1) return c / 2 * (t * t * (((s *= (1.525)) + 1) * t - s)) + b;
                return c / 2*((t -= 2) * t * (((s *= (1.525)) + 1) * t + s) + 2) + b;
            }
        },
        Bounce: {
            easeIn: function(t, b, c, d) {
                return c - Tween.Bounce.easeOut(d-t, 0, c, d) + b;
            },
            easeOut: function(t, b, c, d) {
                if ((t /= d) < (1 / 2.75)) {
                    return c * (7.5625 * t * t) + b;
                } else if (t < (2 / 2.75)) {
                    return c * (7.5625 * (t -= (1.5 / 2.75)) * t + .75) + b;
                } else if (t < (2.5 / 2.75)) {
                    return c * (7.5625 * (t -= (2.25 / 2.75)) * t + .9375) + b;
                } else {
                    return c * (7.5625 * (t -= (2.625 / 2.75)) * t + .984375) + b;
                }
            },
            easeInOut: function(t, b, c, d) {
                if (t < d / 2) {
                    return Tween.Bounce.easeIn(t * 2, 0, c, d) * .5 + b;
                } else {
                    return Tween.Bounce.easeOut(t * 2 - d, 0, c, d) * .5 + c * .5 + b;
                }
            }
        }
    }
    var noPxArr = ["opacity","z-index","zIndex"],
        defaultArr = ["top", "right", "bottom", "left", "width", "height", "margin", "margin-top", "marginTop", "margin-right", "marginRight", "margin-bottom", "marginBottom", "margin-left", "marginLeft", "padding", "padding-top", "paddingTop", "padding-right", "paddingRight", "padding-bottom", "paddingBottom", "padding-left", "paddingLeft", "borderRadius", "border-radius", "border-top-left-radius", "borderTopLeftRadius", "border-top-right-radius", "borderTopRightRadius", "border-bottom-right-radius", "borderBottomRightRadius", "border-bottom-left-radius", "borderBottomLeftRadius"];
    
    function Move(obj, myJson, time, tw, curve, cb) {
        time = time || 1000; // time没有传值,自动添加一个默认值: 1000
        tw = tw || "Quad"; // tw没有传值,自动添加一个默认值: "Quad"
        curve = curve || "easeInOut"; // curve没有传值,自动添加一个默认值: "easeInOut"
        var sVal = {},
            S = {};
            
        for (var attr in myJson) {
    
            // 获取初始值
            sVal[attr] = parseFloat((obj.currentStyle || getComputedStyle(obj))[attr]);

            // css中没写默认样式属性值等于0
            for (var i = 0, len = defaultArr.length; i < len; i++) {
                if (attr === defaultArr[i] && isNaN(sVal[attr])) {
                    sVal[attr] = 0;
                    break;
                }
            }

            // 判断opacity有没有默认值没有就赋值1
            if (attr.toLocaleLowerCase() === "opacity" && isNaN(sVal[attr])) {
                sVal[attr] = 1;
            }

            // 获取总路程
            S[attr] = myJson[attr] - sVal[attr];
        }

        //得到初始时间
        var date = new Date();
        
        function run() {

            // 获取当前时间
            var nowTime = new Date() - date;

            // 如果当前时间的数值 >= 设置的时间则当前的数值=设置的时间
            if (nowTime >= time) {
                nowTime = time;

                // cb && cb.call(obj); 不能写在这里,因为nowTime = time;之后还有一个赋值过程,吧回调函数执行的代码覆盖了
            } else {

                // 没有到规定时间就不停地调用函数run
                requestAnimationFrame(run);
            };
            
            for (var attr in myJson) {
                
                // 存储最终数值
                var ss = Tween[tw][curve](nowTime, sVal[attr], S[attr], time);

                // 利用一个开关进行判断此次属性值是否加单位px
                var bool = false;
                
                for (var i = 0, len = noPxArr.length; i < len; i++) {
                    bool = attr === noPxArr[i];
                    if (bool) {

                        // 跳出本次循环
                        break;
                    }
                }

                // 给obj节点对象赋值
                obj.style[attr] = ss + (bool ? "" : "px");
                
                // 给ie8以下兼容opacity
                if (attr.toLocaleLowerCase() === "opacity") {
                    obj.style["filter"] = "alpha(opacity="+ss*100+")";
                }
            }

            if (nowTime === time) {
                cb && cb.call(obj);
            }
        }
        run();
    }
    window.Move = Move;

}());