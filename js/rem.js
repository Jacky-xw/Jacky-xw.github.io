(function(win, doc){
    var docEl = doc.documentElement || document.body;
    var resize = 'orientationchange' in win ? 'orientationchange' : 'resize';
    function rem(){
        var w = docEl.clientWidth/720>1 ? 720 : docEl.clientWidth;
        docEl.style.fontSize = 100*(w/720) + 'px';
    }
    doc.addEventListener('DOMContentLoaded', rem, false);
    win.addEventListener(resize, rem, false);
})(window, document)