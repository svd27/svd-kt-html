'use strict';var Kotlin = Object.create(null);
(function() {
  function e() {
    return function b() {
      var c = Object.create(b.proto), h = b.initializer;
      null != h && (0 == h.length ? h.call(c) : h.apply(c, arguments));
      Object.seal(c);
      return c
    }
  }
  function f(a, b) {
    for(var c = null, h = 0, e = a.length;h < e;h++) {
      var d = a[h], g = d.proto;
      null === g || null === d.properties || (null === c ? c = Object.create(g, b || void 0) : Object.defineProperties(c, d.properties))
    }
    return c
  }
  function d(a, b, c, h) {
    var d;
    null === a ? (d = null, a = !h && null === c ? null : Object.create(null, c || void 0)) : Array.isArray(a) ? (d = a[0].initializer, a = f(a, c), null === a && h && (a = Object.create(null, c || void 0))) : (d = a.initializer, a = !h && null === c ? a.proto : Object.create(a.proto, c || void 0));
    var g = e();
    Object.defineProperty(g, "proto", {value:a});
    Object.defineProperty(g, "properties", {value:c || null});
    h && (Object.defineProperty(g, "initializer", {value:b}), Object.defineProperty(b, "baseInitializer", {value:d}), Object.freeze(b));
    Object.freeze(g);
    return g
  }
  function g(a, b) {
    return function() {
      if(null !== b) {
        var c = b;
        b = null;
        c.call(a);
        Object.seal(a)
      }
      return a
    }
  }
  Kotlin.argumentsToArrayLike = function(a) {
    return a
  };
  Kotlin.keys = Object.keys;
  Kotlin.isType = function(a, b) {
    return null === a || void 0 === a ? !1 : Object.getPrototypeOf(a) == b.proto ? !0 : !1
  };
  Kotlin.createTrait = function(a, b) {
    return d(a, null, b, !1)
  };
  Kotlin.createClass = function(a, b, c) {
    return d(a, null === b ? function() {
    } : b, c, !0)
  };
  Kotlin.createObject = function(a, b, c) {
    c = Object.create(null === a ? null : Array.isArray(a) ? f(a, c) : a.proto, c || void 0);
    null !== b && (null !== a && Object.defineProperty(b, "baseInitializer", {value:Array.isArray(a) ? a[0].initializer : a.initializer}), b.call(c));
    Object.seal(c);
    return c
  };
  Kotlin.definePackage = function(a, b) {
    var c = Object.create(null, null === b ? void 0 : b);
    if(null === a) {
      return{value:c}
    }
    c = g(c, a);
    Object.freeze(c);
    return{get:c}
  };
  Kotlin.$new = function(a) {
    return a
  };
  Kotlin.$createClass = function(a, b) {
    null !== a && "function" != typeof a && (b = a, a = null);
    var c = null, h = b ? {} : null;
    if(null != h) {
      for(var d = Object.getOwnPropertyNames(b), e = 0, g = d.length;e < g;e++) {
        var f = d[e], i = b[f];
        "initialize" == f ? c = i : 0 === f.indexOf("get_") ? (h[f.substring(4)] = {get:i}, h[f] = {value:i}) : 0 === f.indexOf("set_") ? (h[f.substring(4)] = {set:i}, h[f] = {value:i}) : h[f] = {value:i, writable:!0}
      }
    }
    return Kotlin.createClass(a || null, c, h)
  };
  Kotlin.defineModule = function(a, b) {
    if(a in Kotlin.modules) {
      throw Kotlin.$new(Kotlin.IllegalArgumentException)();
    }
    Object.freeze(b);
    Object.defineProperty(Kotlin.modules, a, {value:b})
  }
})();
String.prototype.startsWith = function(e) {
  return 0 === this.indexOf(e)
};
String.prototype.endsWith = function(e) {
  return-1 !== this.indexOf(e, this.length - e.length)
};
String.prototype.contains = function(e) {
  return-1 !== this.indexOf(e)
};
var kotlin = {set:function(e, f, d) {
  return e.put(f, d)
}};
(function() {
  function e(a) {
    return function() {
      throw new TypeError(void 0 !== a ? "Function " + a + " is abstract" : "Function is abstract");
    }
  }
  Kotlin.equals = function(a, b) {
    if(null === a || void 0 === a) {
      return null === b
    }
    if(a instanceof Array) {
      if(!(b instanceof Array) || a.length != b.length) {
        return!1
      }
      for(var c = 0;c < a.length;c++) {
        if(!Kotlin.equals(a[c], b[c])) {
          return!1
        }
      }
      return!0
    }
    return"object" == typeof a && void 0 !== a.equals ? a.equals(b) : a === b
  };
  Kotlin.array = function(a) {
    return null === a || void 0 === a ? [] : a.slice()
  };
  Kotlin.intUpto = function(a, b) {
    return Kotlin.$new(Kotlin.NumberRange)(a, b)
  };
  Kotlin.intDownto = function(a, b) {
    return Kotlin.$new(Kotlin.Progression)(a, b, -1)
  };
  Kotlin.modules = {};
  Kotlin.Exception = Kotlin.$createClass();
  Kotlin.RuntimeException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.IndexOutOfBounds = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.NullPointerException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.NoSuchElementException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.IllegalArgumentException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.IllegalStateException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.IndexOutOfBoundsException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.UnsupportedOperationException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.IOException = Kotlin.$createClass(Kotlin.Exception);
  Kotlin.throwNPE = function() {
    throw Kotlin.$new(Kotlin.NullPointerException)();
  };
  Kotlin.Iterator = Kotlin.$createClass({initialize:function() {
  }, next:e("Iterator#next"), hasNext:e("Iterator#hasNext")});
  var f = Kotlin.$createClass(Kotlin.Iterator, {initialize:function(a) {
    this.array = a;
    this.size = a.length;
    this.index = 0
  }, next:function() {
    return this.array[this.index++]
  }, hasNext:function() {
    return this.index < this.size
  }}), d = Kotlin.$createClass(f, {initialize:function(a) {
    this.list = a;
    this.size = a.size();
    this.index = 0
  }, next:function() {
    return this.list.get(this.index++)
  }});
  Kotlin.Collection = Kotlin.$createClass();
  Kotlin.AbstractCollection = Kotlin.$createClass(Kotlin.Collection, {size:function() {
    return this.$size
  }, addAll:function(a) {
    for(var a = a.iterator(), b = this.size();0 < b--;) {
      this.add(a.next())
    }
  }, isEmpty:function() {
    return 0 === this.size()
  }, iterator:function() {
    return Kotlin.$new(f)(this.toArray())
  }, equals:function(a) {
    if(this.size() === a.size()) {
      for(var b = this.iterator(), a = a.iterator(), c = this.size();0 < c--;) {
        if(!Kotlin.equals(b.next(), a.next())) {
          return!1
        }
      }
    }
    return!0
  }, toString:function() {
    for(var a = "[", b = this.iterator(), c = !0, h = this.$size;0 < h--;) {
      c ? c = !1 : a += ", ", a += b.next()
    }
    return a + "]"
  }, toJSON:function() {
    return this.toArray()
  }});
  Kotlin.AbstractList = Kotlin.$createClass(Kotlin.AbstractCollection, {iterator:function() {
    return Kotlin.$new(d)(this)
  }, remove:function(a) {
    a = this.indexOf(a);
    -1 !== a && this.removeAt(a)
  }, contains:function(a) {
    return-1 !== this.indexOf(a)
  }});
  Kotlin.ArrayList = Kotlin.$createClass(Kotlin.AbstractList, {initialize:function() {
    this.array = [];
    this.$size = 0
  }, get:function(a) {
    this.checkRange(a);
    return this.array[a]
  }, set:function(a, b) {
    this.checkRange(a);
    this.array[a] = b
  }, size:function() {
    return this.$size
  }, iterator:function() {
    return Kotlin.arrayIterator(this.array)
  }, add:function(a) {
    this.array[this.$size++] = a
  }, addAt:function(a, b) {
    this.array.splice(a, 0, b);
    this.$size++
  }, addAll:function(a) {
    for(var b = a.iterator(), c = this.$size, h = a.size();0 < h--;) {
      this.array[c++] = b.next()
    }
    this.$size += a.size()
  }, removeAt:function(a) {
    this.checkRange(a);
    this.$size--;
    return this.array.splice(a, 1)[0]
  }, clear:function() {
    this.$size = this.array.length = 0
  }, indexOf:function(a) {
    for(var b = 0, c = this.$size;b < c;++b) {
      if(Kotlin.equals(this.array[b], a)) {
        return b
      }
    }
    return-1
  }, toArray:function() {
    return this.array.slice(0, this.$size)
  }, toString:function() {
    return"[" + this.array.join(", ") + "]"
  }, toJSON:function() {
    return this.array
  }, checkRange:function(a) {
    if(0 > a || a >= this.$size) {
      throw new Kotlin.IndexOutOfBoundsException;
    }
  }});
  Kotlin.Runnable = Kotlin.$createClass({initialize:function() {
  }, run:e("Runnable#run")});
  Kotlin.Comparable = Kotlin.$createClass({initialize:function() {
  }, compareTo:e("Comparable#compareTo")});
  Kotlin.Appendable = Kotlin.$createClass({initialize:function() {
  }, append:e("Appendable#append")});
  Kotlin.Closeable = Kotlin.$createClass({initialize:function() {
  }, close:e("Closeable#close")});
  Kotlin.parseInt = function(a) {
    return parseInt(a, 10)
  };
  Kotlin.safeParseInt = function(a) {
    a = parseInt(a, 10);
    return isNaN(a) ? null : a
  };
  Kotlin.safeParseDouble = function(a) {
    a = parseFloat(a);
    return isNaN(a) ? null : a
  };
  Kotlin.System = function() {
    var a = "", b = function(b) {
      void 0 !== b && (a = null === b || "object" !== typeof b ? a + b : a + b.toString())
    }, c = function(b) {
      this.print(b);
      a += "\n"
    };
    return{out:function() {
      return{print:b, println:c}
    }, output:function() {
      return a
    }, flush:function() {
      a = ""
    }}
  }();
  Kotlin.println = function(a) {
    Kotlin.System.out().println(a)
  };
  Kotlin.print = function(a) {
    Kotlin.System.out().print(a)
  };
  Kotlin.RangeIterator = Kotlin.$createClass(Kotlin.Iterator, {initialize:function(a, b, c) {
    this.$start = a;
    this.$end = b;
    this.$increment = c;
    this.$i = a
  }, get_start:function() {
    return this.$start
  }, get_end:function() {
    return this.$end
  }, get_i:function() {
    return this.$i
  }, set_i:function(a) {
    this.$i = a
  }, next:function() {
    var a = this.$i;
    this.set_i(this.$i + this.$increment);
    return a
  }, hasNext:function() {
    return 0 < this.get_count()
  }});
  Kotlin.NumberRange = Kotlin.$createClass({initialize:function(a, b) {
    this.$start = a;
    this.$end = b
  }, get_start:function() {
    return this.$start
  }, get_end:function() {
    return this.$end
  }, get_increment:function() {
    return 1
  }, contains:function(a) {
    return this.$start <= a && a <= this.$end
  }, iterator:function() {
    return Kotlin.$new(Kotlin.RangeIterator)(this.get_start(), this.get_end())
  }});
  Kotlin.Progression = Kotlin.$createClass({initialize:function(a, b, c) {
    this.$start = a;
    this.$end = b;
    this.$increment = c
  }, get_start:function() {
    return this.$start
  }, get_end:function() {
    return this.$end
  }, get_increment:function() {
    return this.$increment
  }, iterator:function() {
    return Kotlin.$new(Kotlin.RangeIterator)(this.get_start(), this.get_end(), this.get_increment())
  }});
  Kotlin.Comparator = Kotlin.$createClass({initialize:function() {
  }, compare:e("Comparator#compare")});
  var g = Kotlin.$createClass(Kotlin.Comparator, {initialize:function(a) {
    this.compare = a
  }});
  Kotlin.comparator = function(a) {
    return Kotlin.$new(g)(a)
  };
  Kotlin.collectionsMax = function(a, b) {
    var c = a.iterator();
    if(a.isEmpty()) {
      throw Kotlin.Exception();
    }
    for(var h = c.next();c.hasNext();) {
      var d = c.next();
      0 > b.compare(h, d) && (h = d)
    }
    return h
  };
  Kotlin.collectionsSort = function(a, b) {
    var c = void 0;
    void 0 !== b && (c = b.compare.bind(b));
    a instanceof Array && a.sort(c);
    for(var h = [], d = a.iterator();d.hasNext();) {
      h.push(d.next())
    }
    h.sort(c);
    c = 0;
    for(d = h.length;c < d;c++) {
      a.set(c, h[c])
    }
  };
  Kotlin.StringBuilder = Kotlin.$createClass({initialize:function() {
    this.string = ""
  }, append:function(a) {
    this.string += a.toString()
  }, toString:function() {
    return this.string
  }});
  Kotlin.splitString = function(a, b) {
    return a.split(b)
  };
  Kotlin.nullArray = function(a) {
    for(var b = [];0 < a;) {
      b[--a] = null
    }
    return b
  };
  Kotlin.numberArrayOfSize = function(a) {
    return Kotlin.arrayFromFun(a, function() {
      return 0
    })
  };
  Kotlin.charArrayOfSize = function(a) {
    return Kotlin.arrayFromFun(a, function() {
      return"\x00"
    })
  };
  Kotlin.booleanArrayOfSize = function(a) {
    return Kotlin.arrayFromFun(a, function() {
      return!1
    })
  };
  Kotlin.arrayFromFun = function(a, b) {
    for(var c = Array(a), d = 0;d < a;d++) {
      c[d] = b(d)
    }
    return c
  };
  Kotlin.arrayIndices = function(a) {
    return Kotlin.$new(Kotlin.NumberRange)(0, a.length - 1)
  };
  Kotlin.arrayIterator = function(a) {
    return Kotlin.$new(f)(a)
  };
  Kotlin.toString = function(a) {
    return a.toString()
  };
  Kotlin.jsonFromPairs = function(a) {
    for(var b = a.length, c = {};0 < b;) {
      --b, c[a[b][0]] = a[b][1]
    }
    return c
  };
  Kotlin.jsonSet = function(a, b, c) {
    a[b] = c
  };
  Kotlin.jsonGet = function(a, b) {
    return a[b]
  };
  Kotlin.jsonAddProperties = function(a, b) {
    for(var c in b) {
      b.hasOwnProperty(c) && (a[c] = b[c])
    }
    return a
  };
  Kotlin.sure = function(a) {
    return a
  };
  Kotlin.concat = function(a, b) {
    for(var c = Array(a.length + b.length), d = 0, e = a.length;d < e;d++) {
      c[d] = a[d]
    }
    for(var e = b.length, g = 0;g < e;) {
      c[d++] = b[g++]
    }
    return c
  }
})();
Kotlin.assignOwner = function(e, f) {
  e.o = f;
  return e
};
Kotlin.b0 = function(e, f, d) {
  return function() {
    return e.call(null !== f ? f : this, d)
  }
};
Kotlin.b1 = function(e, f, d) {
  return function() {
    return e.apply(null !== f ? f : this, d)
  }
};
Kotlin.b2 = function(e, f, d) {
  return function() {
    return e.apply(null !== f ? f : this, Kotlin.concat(d, arguments))
  }
};
Kotlin.b3 = function(e, f) {
  return function() {
    return e.call(f)
  }
};
Kotlin.b4 = function(e, f) {
  return function() {
    return e.apply(f, Kotlin.argumentsToArrayLike(arguments))
  }
};
(function() {
  function e(a) {
    return"string" == typeof a ? a : typeof a.hashCode == j ? (a = a.hashCode(), "string" == typeof a ? a : e(a)) : typeof a.toString == j ? a.toString() : "" + a
  }
  function f(a, b) {
    return a.equals(b)
  }
  function d(a, b) {
    return typeof b.equals == j ? b.equals(a) : a === b
  }
  function g(a) {
    return function(b) {
      if(null === b) {
        throw Error("null is not a valid " + a);
      }
      if("undefined" == typeof b) {
        throw Error(a + " must not be undefined");
      }
    }
  }
  function a(a, b, c, d) {
    this[0] = a;
    this.entries = [];
    this.addEntry(b, c);
    null !== d && (this.getEqualityFunction = function() {
      return d
    })
  }
  function b(a) {
    return function(b) {
      for(var c = this.entries.length, d, e = this.getEqualityFunction(b);c--;) {
        if(d = this.entries[c], e(b, d[0])) {
          switch(a) {
            case i:
              return!0;
            case l:
              return d;
            case p:
              return[c, d[1]]
          }
        }
      }
      return!1
    }
  }
  function c(a) {
    return function(b) {
      for(var c = b.length, d = 0, e = this.entries.length;d < e;++d) {
        b[c + d] = this.entries[d][a]
      }
    }
  }
  function h(b, c) {
    var d = b[c];
    return d && d instanceof a ? d : null
  }
  var j = "function", n = typeof Array.prototype.splice == j ? function(a, b) {
    a.splice(b, 1)
  } : function(a, b) {
    var c, d, e;
    if(b === a.length - 1) {
      a.length = b
    }else {
      c = a.slice(b + 1);
      a.length = b;
      d = 0;
      for(e = c.length;d < e;++d) {
        a[b + d] = c[d]
      }
    }
  }, k = g("key"), o = g("value"), i = 0, l = 1, p = 2;
  a.prototype = {getEqualityFunction:function(a) {
    return typeof a.equals == j ? f : d
  }, getEntryForKey:b(l), getEntryAndIndexForKey:b(p), removeEntryForKey:function(a) {
    return(a = this.getEntryAndIndexForKey(a)) ? (n(this.entries, a[0]), a[1]) : null
  }, addEntry:function(a, b) {
    this.entries[this.entries.length] = [a, b]
  }, keys:c(0), values:c(1), getEntries:function(a) {
    for(var b = a.length, c = 0, d = this.entries.length;c < d;++c) {
      a[b + c] = this.entries[c].slice(0)
    }
  }, containsKey:b(i), containsValue:function(a) {
    for(var b = this.entries.length;b--;) {
      if(a === this.entries[b][1]) {
        return!0
      }
    }
    return!1
  }};
  var q = function(b, c) {
    var d = this, g = [], f = {}, i = typeof b == j ? b : e, l = typeof c == j ? c : null;
    this.put = function(b, c) {
      k(b);
      o(c);
      var d = i(b), e, j = null;
      (e = h(f, d)) ? (d = e.getEntryForKey(b)) ? (j = d[1], d[1] = c) : e.addEntry(b, c) : (e = new a(d, b, c, l), g[g.length] = e, f[d] = e);
      return j
    };
    this.get = function(a) {
      k(a);
      var b = i(a);
      if(b = h(f, b)) {
        if(a = b.getEntryForKey(a)) {
          return a[1]
        }
      }
      return null
    };
    this.containsKey = function(a) {
      k(a);
      var b = i(a);
      return(b = h(f, b)) ? b.containsKey(a) : !1
    };
    this.containsValue = function(a) {
      o(a);
      for(var b = g.length;b--;) {
        if(g[b].containsValue(a)) {
          return!0
        }
      }
      return!1
    };
    this.clear = function() {
      g.length = 0;
      f = {}
    };
    this.isEmpty = function() {
      return!g.length
    };
    var m = function(a) {
      return function() {
        for(var b = [], c = g.length;c--;) {
          g[c][a](b)
        }
        return b
      }
    };
    this._keys = m("keys");
    this._values = m("values");
    this._entries = m("getEntries");
    this.values = function() {
      for(var a = this._values(), b = a.length, c = Kotlin.$new(Kotlin.ArrayList)();b--;) {
        c.add(a[b])
      }
      return c
    };
    this.remove = function(a) {
      k(a);
      var b = i(a), c = null, d = h(f, b);
      if(d && (c = d.removeEntryForKey(a), null !== c && !d.entries.length)) {
        a: {
          for(a = g.length;a--;) {
            if(d = g[a], b === d[0]) {
              break a
            }
          }
          a = null
        }
        n(g, a);
        delete f[b]
      }
      return c
    };
    this.size = function() {
      for(var a = 0, b = g.length;b--;) {
        a += g[b].entries.length
      }
      return a
    };
    this.each = function(a) {
      for(var b = d._entries(), c = b.length, e;c--;) {
        e = b[c], a(e[0], e[1])
      }
    };
    this.putAll = function(a, b) {
      for(var c = a._entries(), e, g, h, f = c.length, i = typeof b == j;f--;) {
        e = c[f];
        g = e[0];
        e = e[1];
        if(i && (h = d.get(g))) {
          e = b(g, h, e)
        }
        d.put(g, e)
      }
    };
    this.clone = function() {
      var a = new q(b, c);
      a.putAll(d);
      return a
    };
    this.keySet = function() {
      for(var a = Kotlin.$new(Kotlin.ComplexHashSet)(), b = this._keys(), c = b.length;c--;) {
        a.add(b[c])
      }
      return a
    }
  };
  Kotlin.HashTable = q
})();
Kotlin.Map = Kotlin.$createClass();
Kotlin.HashMap = Kotlin.$createClass(Kotlin.Map, {initialize:function() {
  Kotlin.HashTable.call(this)
}});
Kotlin.ComplexHashMap = Kotlin.HashMap;
(function() {
  var e = Kotlin.$createClass(Kotlin.Iterator, {initialize:function(d, e) {
    this.map = d;
    this.keys = e;
    this.size = e.length;
    this.index = 0
  }, next:function() {
    return this.map[this.keys[this.index++]]
  }, hasNext:function() {
    return this.index < this.size
  }}), f = Kotlin.$createClass(Kotlin.Collection, {initialize:function(d) {
    this.map = d
  }, iterator:function() {
    return Kotlin.$new(e)(this.map.map, Kotlin.keys(this.map.map))
  }, isEmpty:function() {
    return 0 === this.map.$size
  }, contains:function(d) {
    return this.map.containsValue(d)
  }});
  Kotlin.PrimitiveHashMap = Kotlin.$createClass(Kotlin.Map, {initialize:function() {
    this.$size = 0;
    this.map = {}
  }, size:function() {
    return this.$size
  }, isEmpty:function() {
    return 0 === this.$size
  }, containsKey:function(d) {
    return void 0 !== this.map[d]
  }, containsValue:function(d) {
    var e = this.map, a;
    for(a in e) {
      if(e.hasOwnProperty(a) && e[a] === d) {
        return!0
      }
    }
    return!1
  }, get:function(d) {
    return this.map[d]
  }, put:function(d, e) {
    var a = this.map[d];
    this.map[d] = void 0 === e ? null : e;
    void 0 === a && this.$size++;
    return a
  }, remove:function(d) {
    var e = this.map[d];
    void 0 !== e && (delete this.map[d], this.$size--);
    return e
  }, clear:function() {
    this.$size = 0;
    this.map = {}
  }, putAll:function(d) {
    var d = d.map, e;
    for(e in d) {
      d.hasOwnProperty(e) && (this.map[e] = d[e], this.$size++)
    }
  }, keySet:function() {
    var d = Kotlin.$new(Kotlin.PrimitiveHashSet)(), e = this.map, a;
    for(a in e) {
      e.hasOwnProperty(a) && d.add(a)
    }
    return d
  }, values:function() {
    return Kotlin.$new(f)(this)
  }, toJSON:function() {
    return this.map
  }})
})();
Kotlin.Set = Kotlin.$createClass(Kotlin.Collection);
Kotlin.PrimitiveHashSet = Kotlin.$createClass(Kotlin.AbstractCollection, {initialize:function() {
  this.$size = 0;
  this.map = {}
}, contains:function(e) {
  return!0 === this.map[e]
}, add:function(e) {
  var f = this.map[e];
  this.map[e] = !0;
  if(!0 === f) {
    return!1
  }
  this.$size++;
  return!0
}, remove:function(e) {
  return!0 === this.map[e] ? (delete this.map[e], this.$size--, !0) : !1
}, clear:function() {
  this.$size = 0;
  this.map = {}
}, toArray:function() {
  return Kotlin.keys(this.map)
}});
(function() {
  function e(f, d) {
    var g = new Kotlin.HashTable(f, d);
    this.add = function(a) {
      g.put(a, !0)
    };
    this.addAll = function(a) {
      for(var b = a.length;b--;) {
        g.put(a[b], !0)
      }
    };
    this.values = function() {
      return g._keys()
    };
    this.iterator = function() {
      return Kotlin.arrayIterator(this.values())
    };
    this.remove = function(a) {
      return g.remove(a) ? a : null
    };
    this.contains = function(a) {
      return g.containsKey(a)
    };
    this.clear = function() {
      g.clear()
    };
    this.size = function() {
      return g.size()
    };
    this.isEmpty = function() {
      return g.isEmpty()
    };
    this.clone = function() {
      var a = new e(f, d);
      a.addAll(g.keys());
      return a
    };
    this.equals = function(a) {
      if(null === a || void 0 === a) {
        return!1
      }
      if(this.size() === a.size()) {
        for(var b = this.iterator(), a = a.iterator();;) {
          var c = b.hasNext(), d = a.hasNext();
          if(c != d) {
            break
          }
          if(d) {
            if(c = b.next(), d = a.next(), !Kotlin.equals(c, d)) {
              break
            }
          }else {
            return!0
          }
        }
      }
      return!1
    };
    this.toString = function() {
      for(var a = "[", b = this.iterator(), c = !0;b.hasNext();) {
        c ? c = !1 : a += ", ", a += b.next()
      }
      return a + "]"
    };
    this.intersection = function(a) {
      for(var b = new e(f, d), a = a.values(), c = a.length, h;c--;) {
        h = a[c], g.containsKey(h) && b.add(h)
      }
      return b
    };
    this.union = function(a) {
      for(var b = this.clone(), a = a.values(), c = a.length, d;c--;) {
        d = a[c], g.containsKey(d) || b.add(d)
      }
      return b
    };
    this.isSubsetOf = function(a) {
      for(var b = g.keys(), c = b.length;c--;) {
        if(!a.contains(b[c])) {
          return!1
        }
      }
      return!0
    }
  }
  Kotlin.HashSet = Kotlin.$createClass(Kotlin.Set, {initialize:function() {
    e.call(this)
  }});
  Kotlin.ComplexHashSet = Kotlin.HashSet
})();

