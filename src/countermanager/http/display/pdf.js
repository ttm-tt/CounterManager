/* Copyright (C) 2020 Christoph Theis */

/*
 * Parameter
 * pdf: Filename of PDF file. file:/// - URLs are not allowed!
 * timeout: time to show each page
 * startPage: first page to show
 * endPage: last page to show
 * noUpdate: No update, default 0
 */

var fileName = getParameterByName('pdf', undefined);

$(document).ready(function() {
    if (fileName === undefined)
        return;
    
    update();
});


function update() {
    if (parent != this && !parent.show())
        return;
    
    PDFJS.disableStream = true;
    
    PDFJS.getDocument(fileName).then(function(pdfFile) {
        showPage(pdfFile, getParameterByName('startPage', 1));
    });
}


function showPage(pdfFile, pageNumber) {
    if (pageNumber > getParameterByName('endPage', pdfFile.numPages + 1)) {
        update();
        
        return;
    }
    
    var content = document.getElementById('content');
    
    var canvas = document.createElement('canvas');
    canvas.setAttribute('id', 'pdf');

    var context = canvas.getContext('2d');

    pdfFile.getPage(pageNumber).then(function(page) {
        viewport = page.getViewport(1);

        var scaleW = content.clientWidth / viewport.width;
        var scaleH = content.clientHeight / viewport.height;
        var scale = Math.min(scaleW, scaleH);

        viewport = page.getViewport(scale);

        canvas.height = viewport.height;
        canvas.width = viewport.width;

        var renderContext = {
            canvasContext: context,
            viewport: viewport
        };

        page.render(renderContext).promise.then(function(e) {
            content.removeChild(document.getElementById('pdf'));
            content.appendChild(canvas);
            
            // Show next data (if there is any)
            if (getParameterByName('noUpdate', 0) == 0)
                setTimeout(function() {showPage(pdfFile, pageNumber + 1);}, getParameterByName('timeout', 4) * 1000);
            });            
        });
        
};