/* 
* @param
*       obj -- object 节点对象
*       cName -- string 要删除的类名
* @return
*       undefined
*/
function removeClass(obj, cName) {
    var startName = obj.className.split(" "),
        nowArr = cName.split(" "),
        len1 = startName.length,
        len2 = nowArr.length,
        newArr = [];
    for (var i = 0; i < len1; i++) {
        for (var j = 0; j < len2; j++) {
            startName[i] === nowArr[j] && delete startName[i];
        }
        startName[i] && (newArr[i] = startName[i]);
    }
    obj.className = newArr.join(" ");
}