/**
 * Ctrl + Q 或 按住页面拉取
 */
function screenShot(){

    // 清除body的默认样式
    document.body.style.margin = 0;
    if ((document.body.currentStyle || getComputedStyle(document.body)).overflow === "visible") {
        document.body.style.overflow = "hidden";
    }

    // 兼容事件监听 - start
    function addEvent(obj, eType, eFn) {
        if (obj.addEventListener) {
            obj.addEventListener(eType, eFn);
        } else {
            obj.attachEvent("on"+eType, eFn);
        };
    }
    function removeEvent(obj, eType, eFn) {
        if (obj.removeEventListener) {
            obj.removeEventListener(eType, eFn);
        } else {
            obj.detechEvent("on"+eType, eFn);
        };
    }
    // 兼容事件监听 - end

    // 公用代码
    function getoBox() {
        var oBox = document.createElement("div");
        oBox.id = "_screenShot_div";
        oBox.style.cssText = "position: absolute;z-index: 1000;top: 0;left: 0;width: "+document.documentElement.clientWidth+"px;height: "+document.documentElement.clientHeight+"px;background-color: rgba(0, 0, 0, 0.7);";
        document.body.appendChild(oBox);
    }

    // 按下Ctrl + Q
    function domKeydown(e) {
        e = e || window.event;
        if (!document.getElementById("_screenShot_div")) {
            if (e.keyCode === 81 && e.ctrlKey) {
                getoBox();
            }
        } else if (e.keyCode === 81 && e.ctrlKey) {
            var oBox = document.getElementById("_screenShot_div");
            if (oBox) {
                document.body.removeChild(oBox);
            }
            fnn();
        }
    };
    addEvent(document, "keydown", domKeydown);

    // ----- 初次创建大盒子 - start -----
    // document初次按下时
    function fnn() {
        function domDown(e) {
            e = e || window.event;
            var startX = e.clientX,
                startY = e.clientY;

            if (!document.getElementById("_screenShot_div")) {
                getoBox();
            }
            var oBox = document.getElementById("_screenShot_div");
            var oUl = document.createElement("ul");
            oUl.style.cssText = "position: absolute;top: "+startY+"px;left: "+startX+"px;margin: 0;padding: 0;border: 2px solid #007acc;cursor: move;";
            oUl.id = "_screenShot_ul";
            oBox.appendChild(oUl);
            var styleArr = ["top: -5px;left: -5px;cursor: nw-resize;", "top: -5px;left: 50%;margin-left: -4px;cursor: n-resize;", "top: -5px;right: -5px;cursor: ne-resize;", "top: 50%;left: -5px;margin-top: -4px;cursor: w-resize;", "top: 50%;right: -5px;margin-top: -4px;cursor: w-resize;", "bottom: -5px;left: -5px;cursor: ne-resize;", "bottom: -5px;left: 50%;margin-left: -4px;cursor: n-resize;", "bottom: -5px;right: -5px;cursor: nw-resize;"];
            for (var i = 0; i < 8; i++) {
                var li = document.createElement("li");
                li.style.cssText = "list-style: none;position: absolute;width: 8px;height: 8px;background-color: skyblue;" + styleArr[i];
                li.id = "_screenShot_li_" + i;
                oUl.appendChild(li);
            }
            
            function domMove(e) {
                e = e || window.event;
                oUl.style.width = e.clientX - startX + "px";
                oUl.style.height = e.clientY - startY + "px";
            }
            addEvent(document, "mousemove", domMove);
            
            // document初次抬起时
            function domUp() {
                removeEvent(document, "mouseup", domUp);
                removeEvent(document, "mousemove", domMove);
                removeEvent(document, "mousedown", domDown);
                fn();
            }
            addEvent(document, "mouseup", domUp);
        }
        addEvent(document, "mousedown", domDown);
    }
    fnn();
    
    // ----- 初次创建大盒子 - end -----

    // 按下大盒子
    function fn() {
        var oBox = document.getElementById("_screenShot_div"),
            oUl = document.getElementById("_screenShot_ul"),
            aLi = oUl.children;
        function domDown(e) {
            e = e || window.event;
            if (e.preventDefault) {
                e.preventDefault();
            } else {
                e.returnValue = false;
            }
            var target = e.target || e.srcElement,
                startX = e.clientX,
                startY = e.clientY,
                domWidth = document.documentElement.clientWidth,
                domHeight = document.documentElement.clientHeight,
                startWidth = oUl.offsetWidth,
                startHeight = oUl.offsetHeight,
                startLeft = oUl.offsetLeft,
                startTop = oUl.offsetTop;
            
            function domMove(e) {
                e = e || window.event;
                var x = e.clientX - startX,
                    y = e.clientY - startY,
                    min = Math.min,
                    max = Math.max;
                switch (target.id) {
                    case "_screenShot_ul":
                        oUl.style.left = startLeft + x + "px";
                        oUl.style.top = startTop + y + "px";
                        break;
                    case "_screenShot_li_0":
                        x = min(startWidth-100, x);
                        y = min(startHeight-100, y);
                        oUl.style.top = startTop + y + "px";
                        oUl.style.left = startLeft + x + "px";
                        oUl.style.width = min(domWidth-10, startWidth-x) + "px";
                        oUl.style.height = min(domHeight-10, startHeight-y) + "px";
                        break;
                    case "_screenShot_li_1":
                        y = min(startHeight-100, y);
                        oUl.style.top = startTop + y + "px";
                        oUl.style.height = min(domHeight-10, startHeight-y) + "px";
                        break;
                    case "_screenShot_li_2":
                        x = max(-startWidth+100, x);
                        y = min(startHeight-100, y);
                        oUl.style.top = startTop + y + "px";
                        oUl.style.width = min(domWidth-10, startWidth+x) + "px";
                        oUl.style.height = min(domHeight-10, startHeight-y) + "px";
                        break;
                    case "_screenShot_li_3":
                        x = min(startWidth-100, x);
                        oUl.style.left = startLeft + x + "px";
                        oUl.style.width = min(domWidth-10, startWidth-x) + "px";
                        break;
                    case "_screenShot_li_4":
                        x = max(-startWidth+100, x);
                        oUl.style.width = min(domWidth-10, startWidth+x) + "px";
                        break;
                    case "_screenShot_li_5":
                        x = min(startWidth-100, x);
                        y = max(-startHeight+100, y);
                        oUl.style.left = startLeft + x + "px";
                        oUl.style.width = min(domWidth-10, startWidth-x) + "px";
                        oUl.style.height = min(domHeight-10, startHeight+y) + "px";
                        break;
                    case "_screenShot_li_6":
                        y = max(-startHeight+100, y);
                        oUl.style.height = min(domHeight-10, startHeight+y) + "px";
                        break;
                    case "_screenShot_li_7":
                        x = max(-startWidth+100, x);
                        y = max(-startHeight+100, y);
                        oUl.style.width = min(domWidth-10, startWidth+x) + "px";
                        oUl.style.height = min(domHeight-10, startHeight+y) + "px";
                        break;
                };
            }
            function domUp() {
                removeEvent(document, "mouseup", domUp);
                removeEvent(document, "mousemove", domMove);
            }
            addEvent(document, "mouseup", domUp);
            addEvent(document, "mousemove", domMove);
        }
        addEvent(document, "mousedown", domDown);

        // 当窗口发生改变时
        function winResize() {
            oBox.style.width = document.documentElement.clientWidth+"px";
            oBox.style.height = document.documentElement.clientHeight+"px";
        }
        addEvent(window, "resize", winResize);
    }

};