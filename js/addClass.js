/* 
* @param
*       obj -- object 节点对象
*       cName -- string 要添加的类名
* @return
*       undefined
*/
(function (){
    function addClass(obj, cName) {
        var sName = obj.className.split(" "),
            len = sName.length,
            nowName = sName.concat(cName).join(" ");
        for (var i = 0; i < len; i++) {
            if (sName[i].toLocaleLowerCase() === cName) {
                nowName = sName.join(" ");
                break;
            }
        }
        obj.className = nowName;
    }
    window.addClass = addClass;
}());