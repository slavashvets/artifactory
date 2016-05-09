export default {
    INFINITY_VALUE: 'infinite',
    UNSUPPORTED_VALUE: 'unsupported',
    isSpecialValue: function(val) {
        return val === this.INFINITY_VALUE || val === this.UNSUPPORTED_VALUE;
    }
}