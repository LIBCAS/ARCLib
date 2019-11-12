function pad(number) {
    var r = String(number);
    if ( r.length === 1 ) {
        r = '0' + r;
    }
    return r;
}

function toISODateString(date) {
    return date.getUTCFullYear() + '-' + pad(date.getUTCMonth() + 1) + '-' + pad(date.getUTCDate());
};

var today = new Date();

var var1 = 'test';
var var2 = 'test2';
var var3 = toISODateString(today);

var1 + ' ' + var2 + ' ' + var3