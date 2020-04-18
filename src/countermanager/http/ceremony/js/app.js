/*!
 * FlagWaver - App
 * @author krikienoid / https://github.com/krikienoid
 */

(function (THREE) {
    'use strict';

    THREE = THREE && THREE.hasOwnProperty('default') ? THREE['default'] : THREE;

    //
    // Static constants
    //
    // Physics constants
    // ChT: We animate the flags in slow motion: they wave quieter as if the wind speed
    // were lowwer, but they still wouldn't hang down.
    var FPS = 30;
    var TIME_STEP = 1 / FPS;
    var DAMPING = 0.03;
    var DRAG = 1 - DAMPING;

    var G = 9.80665; // m/s^2

    /**
     * Enum for flag hoisting side.
     *
     * @readonly
     * @enum {string}
     * @typedef {string} Hoisting
     */

    var Hoisting = {
      DEXTER: 'dexter',
      SINISTER: 'sinister'
    };
    /**
     * Enum for cardinal directions.
     *
     * @readonly
     * @enum {string}
     * @typedef {string} Side
     */

    var Side = {
      TOP: 'top',
      LEFT: 'left',
      BOTTOM: 'bottom',
      RIGHT: 'right'
    };
    /**
     * Enum for flagpole types.
     *
     * @readonly
     * @enum {string}
     * @typedef {string} FlagpoleType
     */

    var FlagpoleType = {
      VERTICAL: 'vertical',
      HORIZONTAL: 'horizontal',
      OUTRIGGER: 'outrigger',
      CROSSBAR: 'crossbar',
      GALLERY: 'gallery',
      AUSTRALIAN: 'australian'
    };

    function _typeof(obj) {
      if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") {
        _typeof = function (obj) {
          return typeof obj;
        };
      } else {
        _typeof = function (obj) {
          return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj;
        };
      }

      return _typeof(obj);
    }

    function _classCallCheck(instance, Constructor) {
      if (!(instance instanceof Constructor)) {
        throw new TypeError("Cannot call a class as a function");
      }
    }

    function _defineProperties(target, props) {
      for (var i = 0; i < props.length; i++) {
        var descriptor = props[i];
        descriptor.enumerable = descriptor.enumerable || false;
        descriptor.configurable = true;
        if ("value" in descriptor) descriptor.writable = true;
        Object.defineProperty(target, descriptor.key, descriptor);
      }
    }

    function _createClass(Constructor, protoProps, staticProps) {
      if (protoProps) _defineProperties(Constructor.prototype, protoProps);
      if (staticProps) _defineProperties(Constructor, staticProps);
      return Constructor;
    }

    function _inherits(subClass, superClass) {
      if (typeof superClass !== "function" && superClass !== null) {
        throw new TypeError("Super expression must either be null or a function");
      }

      subClass.prototype = Object.create(superClass && superClass.prototype, {
        constructor: {
          value: subClass,
          writable: true,
          configurable: true
        }
      });
      if (superClass) _setPrototypeOf(subClass, superClass);
    }

    function _getPrototypeOf(o) {
      _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) {
        return o.__proto__ || Object.getPrototypeOf(o);
      };
      return _getPrototypeOf(o);
    }

    function _setPrototypeOf(o, p) {
      _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) {
        o.__proto__ = p;
        return o;
      };

      return _setPrototypeOf(o, p);
    }

    function _assertThisInitialized(self) {
      if (self === void 0) {
        throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
      }

      return self;
    }

    function _possibleConstructorReturn(self, call) {
      if (call && (typeof call === "object" || typeof call === "function")) {
        return call;
      }

      return _assertThisInitialized(self);
    }

    function generateDataTexture(width, height, color) {
      var size = width * height;
      var data = new Uint8Array(3 * size);
      var r = Math.floor(color.r * 255);
      var g = Math.floor(color.g * 255);
      var b = Math.floor(color.b * 255);

      for (var i = 0; i < size; i++) {
        var stride = i * 3;
        data[stride] = r;
        data[stride + 1] = g;
        data[stride + 2] = b;
      }

      var texture = new THREE.DataTexture(data, width, height, THREE.RGBFormat);
      return texture;
    }

    var isNumeric = function isNumeric(value) {
      return !isNaN(parseFloat(value)) && isFinite(value);
    }; // Is an object

    var isObject = function isObject(object) {
      return !!(object && _typeof(object) === 'object');
    }; // Object has property

    var hasValue = function hasValue(object, value) {
      return isObject(object) && Object.keys(object).some(function (key) {
        return object[key] === value;
      });
    }; // Is a function

    var depth_frag = "uniform sampler2D texture;\nvarying vec2 vUV;\nvec4 pack_depth(const in float depth) {\n    const vec4 bit_shift = vec4(256.0 * 256.0 * 256.0, 256.0 * 256.0, 256.0, 1.0);\n    const vec4 bit_mask = vec4(0.0, 1.0 / 256.0, 1.0 / 256.0, 1.0 / 256.0);\n    vec4 res = fract(depth * bit_shift);\n    res -= res.xxyz * bit_mask;\n    return res;\n}\nvoid main() {\n    vec4 pixel = texture2D(texture, vUV);\n    if (pixel.a < 0.5) discard;\n    gl_FragData[0] = pack_depth(gl_FragCoord.z);\n}\n";

    var depth_vert = "varying vec2 vUV;\nvoid main() {\n    vUV = 0.75 * uv;\n    vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);\n    gl_Position = projectionMatrix * mvPosition;\n}\n";

    /**
     * @module ShaderChunk
     */

    var ShaderChunk = {
      depth_frag: depth_frag,
      depth_vert: depth_vert
    };

    /**
     * @class Particle
     *
     * @classdesc Represents a mass in a mass-spring system.
     *
     * @param {THREE.Vector3} position
     * @param {number} mass
     */

    var Particle =
    /*#__PURE__*/
    function () {
      function Particle(position, mass) {
        _classCallCheck(this, Particle);

        this.position = position.clone();
        this.previous = position.clone();
        this.original = position.clone();
        this.mass = mass;
        this.inverseMass = 1 / mass;
        this.acceleration = new THREE.Vector3();
        this.tmp = new THREE.Vector3();
      } // Apply force


      _createClass(Particle, [{
        key: "applyForce",
        value: function applyForce(force) {
          this.acceleration.addScaledVector(force, this.inverseMass);
        } // Compute new position

      }, {
        key: "integrate",
        value: function integrate(deltaTimeSq) {
          // Perform verlet integration
          var tmp = this.tmp.subVectors(this.position, this.previous).multiplyScalar(DRAG).add(this.position).addScaledVector(this.acceleration, deltaTimeSq);
          this.tmp = this.previous;
          this.previous = this.position;
          this.position = tmp;
          this.acceleration.set(0, 0, 0);
        }
      }]);

      return Particle;
    }();

    var diff = new THREE.Vector3();
    /**
     * @class Constraint
     *
     * @classdesc Represents a spring constraint in a mass-spring system.
     *
     * @param {Particle} p1
     * @param {Particle} p2
     * @param {number} restDistance
     */

    var Constraint =
    /*#__PURE__*/
    function () {
      function Constraint(p1, p2, restDistance) {
        _classCallCheck(this, Constraint);

        this.p1 = p1;
        this.p2 = p2;
        this.restDistance = restDistance;
      }

      _createClass(Constraint, [{
        key: "resolve",
        value: function resolve() {
          var p1 = this.p1;
          var p2 = this.p2;
          var restDistance = this.restDistance;
          diff.subVectors(p2.position, p1.position);
          var currentDistance = diff.length();

          if (currentDistance === 0) {
            return;
          } // Prevents division by 0


          var correction = diff.multiplyScalar((1 - restDistance / currentDistance) / 2);
          p1.position.add(correction);
          p2.position.sub(correction);
        }
      }]);

      return Constraint;
    }();

    /**
     * @class Cloth
     *
     * @classdesc Simulates the physics of a rectangular cloth using
     * a mass-spring system.
     *
     * @param {number} xSegments - Number of nodes along the x-axis
     * @param {number} ySegments - Number of nodes along the y-axis
     * @param {number} restDistance - Rest distance between adjacent nodes
     * @param {number} mass - Mass of the cloth material
     */

    var Cloth =
    /*#__PURE__*/
    function () {
      function Cloth(xSegments, ySegments, restDistance, mass) {
        _classCallCheck(this, Cloth);

        // Cloth properties
        var width = restDistance * xSegments;
        var height = restDistance * ySegments;
        var particles = [];
        var constraints = []; // Get particle at position (u, v)

        var particleAt = function particleAt(u, v) {
          return particles[u + v * (xSegments + 1)];
        }; // Cloth plane function


        var plane = function plane(u, v, target) {
          target.set(u * width, v * height, 0);
        }; //
        // Particles
        //


        var position = new THREE.Vector3();

        for (var v = 0; v <= ySegments; v++) {
          for (var u = 0; u <= xSegments; u++) {
            plane(u / xSegments, v / ySegments, position);
            particles.push(new Particle(position, mass));
          }
        } //
        // Constraints
        //
        // Structural constraints


        for (var _v = 0; _v < ySegments; _v++) {
          for (var _u = 0; _u < xSegments; _u++) {
            constraints.push(new Constraint(particleAt(_u, _v), particleAt(_u, _v + 1), restDistance));
            constraints.push(new Constraint(particleAt(_u, _v), particleAt(_u + 1, _v), restDistance));
          }
        }

        for (var _u2 = xSegments, _v2 = 0; _v2 < ySegments; _v2++) {
          constraints.push(new Constraint(particleAt(_u2, _v2), particleAt(_u2, _v2 + 1), restDistance));
        }

        for (var _v3 = ySegments, _u3 = 0; _u3 < xSegments; _u3++) {
          constraints.push(new Constraint(particleAt(_u3, _v3), particleAt(_u3 + 1, _v3), restDistance));
        }
        /*
         * While many systems use shear and bend springs, the
         * relax constraints model seems to be just fine using
         * structural springs.
         */
        // Shear constraints


        var diagonalDistance = Math.sqrt(restDistance * restDistance * 2);

        for (var _v4 = 0; _v4 < ySegments; _v4++) {
          for (var _u4 = 0; _u4 < xSegments; _u4++) {
            constraints.push(new Constraint(particleAt(_u4, _v4), particleAt(_u4 + 1, _v4 + 1), diagonalDistance));
            constraints.push(new Constraint(particleAt(_u4 + 1, _v4), particleAt(_u4, _v4 + 1), diagonalDistance));
          }
        } // Bend constraints
        // ChT: Use both, not only shear

        /* */
        //


        var x2Distance = restDistance * 2;
        var y2Distance = restDistance * 2;
        var diagonalDistance2 = Math.sqrt(x2Distance * x2Distance + y2Distance * y2Distance);

        for (var _v5 = 0; _v5 < ySegments - 1; _v5++) {
          for (var _u5 = 0; _u5 < xSegments - 1; _u5++) {
            constraints.push(new Constraint(particleAt(_u5, _v5), particleAt(_u5 + 2, _v5), x2Distance));
            constraints.push(new Constraint(particleAt(_u5, _v5), particleAt(_u5, _v5 + 2), y2Distance));
            constraints.push(new Constraint(particleAt(_u5, _v5), particleAt(_u5 + 2, _v5 + 2), diagonalDistance2));
            constraints.push(new Constraint(particleAt(_u5, _v5 + 2), particleAt(_u5 + 2, _v5 + 2), x2Distance));
            constraints.push(new Constraint(particleAt(_u5 + 2, _v5 + 2), particleAt(_u5 + 2, _v5 + 2), y2Distance));
            constraints.push(new Constraint(particleAt(_u5 + 2, _v5), particleAt(_u5, _v5 + 2), diagonalDistance2));
          }
        } // /* */
        //
        // Geometry
        //


        var geometry = new THREE.ParametricGeometry(plane, xSegments, ySegments, true);
        geometry.dynamic = true;
        geometry.computeFaceNormals(); // Public properties and methods

        this.xSegments = xSegments;
        this.ySegments = ySegments;
        this.restDistance = restDistance;
        this.width = width;
        this.height = height;
        this.particles = particles;
        this.constraints = constraints;
        this.particleAt = particleAt;
        this.geometry = geometry;
      }

      _createClass(Cloth, [{
        key: "reset",
        value: function reset() {
          var particles = this.particles;

          for (var i = 0, ii = particles.length; i < ii; i++) {
            var particle = particles[i];
            particle.previous.copy(particle.position.copy(particle.original));
          }
        }
      }, {
        key: "simulate",
        value: function simulate(deltaTime) {
          var particles = this.particles;
          var constraints = this.constraints;
          var deltaTimeSq = deltaTime * deltaTime; // Compute new particle positions

          for (var i = 0, ii = particles.length; i < ii; i++) {
            particles[i].integrate(deltaTimeSq);
          } // Resolve constraints


          for (var _i = 0, _ii = constraints.length; _i < _ii; _i++) {
            constraints[_i].resolve();
          }
        }
      }, {
        key: "render",
        value: function render() {
          var particles = this.particles;
          var geometry = this.geometry;
          var vertices = geometry.vertices;

          for (var i = 0, ii = particles.length; i < ii; i++) {
            vertices[i].copy(particles[i].position);
          }

          geometry.computeFaceNormals();
          geometry.computeVertexNormals();
          geometry.normalsNeedUpdate = true;
          geometry.verticesNeedUpdate = true;
        }
      }]);

      return Cloth;
    }();

    var SLACK = 1.2;
    var diff$1 = new THREE.Vector3();
    /**
     * @class FixedConstraint
     *
     * @classdesc A unidirectional spring constraint used to mitigate
     * the "super elastic" effect.
     *
     * @param {Particle} p1
     * @param {Particle} p2
     * @param {number} restDistance
     */

    var FixedConstraint =
    /*#__PURE__*/
    function (_Constraint) {
      _inherits(FixedConstraint, _Constraint);

      function FixedConstraint() {
        _classCallCheck(this, FixedConstraint);

        return _possibleConstructorReturn(this, _getPrototypeOf(FixedConstraint).apply(this, arguments));
      }

      _createClass(FixedConstraint, [{
        key: "resolve",
        // Satisfy constraint unidirectionally
        value: function resolve() {
          var p1 = this.p1;
          var p2 = this.p2;
          var restDistance = this.restDistance * SLACK;
          diff$1.subVectors(p1.position, p2.position);
          var currentDistance = diff$1.length() / SLACK;
          diff$1.normalize();
          var correction = diff$1.multiplyScalar(currentDistance - restDistance);

          if (currentDistance > restDistance) {
            p2.position.add(correction);
          }
        }
      }]);

      return FixedConstraint;
    }(Constraint);

    var WHITE_TEXTURE = generateDataTexture(1, 1, new THREE.Color(0xffffff));

    function buildCloth(options) {
      var restDistance = options.height / options.granularity;
      return new Cloth(Math.round(options.width / restDistance), Math.round(options.height / restDistance), restDistance, options.mass);
    }

    function buildMesh(cloth, options) {
      var texture = WHITE_TEXTURE;
      var geometry = cloth.geometry; // Texture

      if (options && options.texture) {
        if (options.texture instanceof THREE.Texture) {
          texture = options.texture;
          texture.needsUpdate = true;
          texture.anisotropy = 16;
          texture.minFilter = THREE.LinearFilter;
          texture.magFilter = THREE.LinearFilter;
          texture.wrapS = texture.wrapT = THREE.ClampToEdgeWrapping;
        } else {
          console.error('FlagWaver.Flag: options.texture must be an instance of THREE.Texture.');
        }
      } // Material


      var material = new THREE.MeshPhongMaterial({
        alphaTest: 0.5,
        color: 0xffffff,
        specular: 0x030303,

        /*
         * shininess cannot be 0 as it causes bugs in some systems.
         * https://github.com/mrdoob/three.js/issues/7252
         */
        shininess: 0.001,
        side: THREE.DoubleSide,
        map: texture
      });
      /* //
      material = new THREE.MeshBasicMaterial({
          color:       0x00ff00,
          wireframe:   true,
          transparent: true,
          opacity:     0.9
      });
      // */
      // Mesh

      var mesh = new THREE.Mesh(geometry, material);
      mesh.castShadow = true;
      mesh.customDepthMaterial = new THREE.ShaderMaterial({
        uniforms: {
          texture: {
            value: texture
          }
        },
        vertexShader: ShaderChunk.depth_vert,
        fragmentShader: ShaderChunk.depth_frag
      });
      return mesh;
    }

    var _pin = function () {
      var defaults = {
        edges: [],
        spacing: 1
      };

      function ensureValidSpacing(spacing) {
        if (isNumeric(spacing) && spacing >= 1) {
          return Math.floor(spacing);
        } else {
          return defaults.spacing;
        }
      }

      function pinEdge(cloth, pins, edge, options) {
        var xSegments = cloth.xSegments,
            ySegments = cloth.ySegments,
            particleAt = cloth.particleAt;
        var spacing = options.spacing;

        switch (edge) {
          case Side.TOP:
            for (var i = 0; i <= xSegments; i += spacing) {
              pins.push(particleAt(i, ySegments));
            }

            break;

          case Side.LEFT:
            for (var _i = 0; _i <= ySegments; _i += spacing) {
              pins.push(particleAt(0, _i));
            }

            break;

          case Side.BOTTOM:
            for (var _i2 = 0; _i2 <= xSegments; _i2 += spacing) {
              pins.push(particleAt(_i2, 0));
            }

            break;

          case Side.RIGHT:
            for (var _i3 = 0; _i3 <= ySegments; _i3 += spacing) {
              pins.push(particleAt(xSegments, _i3));
            }

            break;

          default:
            break;
        }
      }

      return function pin(cloth, pins, options) {
        var settings = Object.assign({}, defaults, options);
        var edges = settings.edges;
        settings.spacing = ensureValidSpacing(settings.spacing);

        if (typeof edges === 'string') {
          // If edges is a string
          pinEdge(cloth, pins, edges, settings);
        } else if (edges && edges.length) {
          // If edges is an array
          for (var i = 0, ii = edges.length; i < ii; i++) {
            pinEdge(cloth, pins, edges[i], settings);
          }
        }
      };
    }();
    /**
     * @class Flag
     *
     * @classdesc Initializes a cloth object to simulate the motion of a flag
     * and applies the cloth geometry to a mesh.
     *
     * @param {Object} [options]
     *   @param {number} [options.width]
     *   @param {number} [options.height]
     *   @param {number} [options.mass]
     *   @param {number} [options.granularity]
     *   @param {THREE.Texture} [options.texture]
     *   @param {Object} [options.pin]
     */


    var Flag =
    /*#__PURE__*/
    function () {
      function Flag(options) {
        _classCallCheck(this, Flag);

        var settings = Object.assign({}, Flag.defaults, options);
        this.cloth = buildCloth(settings);
        this.pins = [];
        this.lengthConstraints = [];
        this.mesh = buildMesh(this.cloth, settings);
        this.mesh.position.set(0, -this.cloth.height, 0);
        this.object = new THREE.Object3D();
        this.object.add(this.mesh);
        this.pin(settings.pin);
      }

      _createClass(Flag, [{
        key: "destroy",
        value: function destroy() {
          if (this.mesh instanceof THREE.Mesh) {
            this.mesh.material.dispose();
            this.mesh.geometry.dispose();
            this.mesh.material.map.dispose();
            this.mesh.customDepthMaterial.dispose();
          }
        }
      }, {
        key: "pin",
        value: function pin(options) {
          _pin(this.cloth, this.pins, options);
        }
      }, {
        key: "unpin",
        value: function unpin() {
          this.pins = [];
        } // Add additional constraints to cloth to mitigate stretching

      }, {
        key: "setLengthConstraints",
        value: function setLengthConstraints(hoistwardSide) {
          var _this$cloth = this.cloth,
              xSegments = _this$cloth.xSegments,
              ySegments = _this$cloth.ySegments,
              restDistance = _this$cloth.restDistance,
              particleAt = _this$cloth.particleAt;
          var lengthConstraints = [];
          /*
           * Order is important, constraints closest to the hoist must be
           * resolved first.
           */

          if (hoistwardSide === Side.LEFT) {
            // Add horizontal constraints that run from hoist to fly
            for (var v = 0; v <= ySegments; v++) {
              for (var u = 0; u < xSegments; u++) {
                lengthConstraints.push(new FixedConstraint(particleAt(u, v), particleAt(u + 1, v), restDistance));
              }
            }
          } else if (hoistwardSide === Side.TOP) {
            // Add vertical constraints that run from top to bottom
            for (var _u = 0; _u <= xSegments; _u++) {
              for (var _v = ySegments; _v > 0; _v--) {
                lengthConstraints.push(new FixedConstraint(particleAt(_u, _v), particleAt(_u, _v - 1), restDistance));
              }
            }
          }

          this.lengthConstraints = lengthConstraints;
        }
      }, {
        key: "reset",
        value: function reset() {
          this.cloth.reset();
        }
      }, {
        key: "simulate",
        value: function simulate(deltaTime) {
          var pins = this.pins;
          var lengthConstraints = this.lengthConstraints;
          this.cloth.simulate(deltaTime); // Pin constraints

          for (var i = 0, ii = pins.length; i < ii; i++) {
            var particle = pins[i];
            particle.previous.copy(particle.position.copy(particle.original));
          } // Length constraints


          for (var _i4 = 0, _ii = lengthConstraints.length; _i4 < _ii; _i4++) {
            lengthConstraints[_i4].resolve();
          }
        }
      }, {
        key: "render",
        value: function render() {
          this.cloth.render();
        }
      }]);

      return Flag;
    }();

    Flag.defaults = {
      width: 300,
      height: 200,
      mass: 0.1,
      granularity: 10,
      rigidness: 1,
      texture: WHITE_TEXTURE,
      pin: {
        edges: [Side.LEFT]
      }
    };

    /**
     * @module WindModifiers
     *
     * @description A collection of optional functions for customizing
     * wind behavior.
     */
    var WindModifiers = {
      noEffect: function noEffect(x) {
        return x;
      },
      blowFromLeftDirection: function blowFromLeftDirection(direction, time) {
        return direction.set(2000, 0, 1000);
      },
      rotatingDirection: function rotatingDirection(direction, time) {
        return direction.set(Math.sin(time / 2000), Math.cos(time / 3000), Math.sin(time / 1000));
      },
      constantSpeed: function constantSpeed(speed, time) {
        return speed;
      },
      variableSpeed: function variableSpeed(speed, time) {
        return Math.cos(time / 7000) * (speed / 2) + speed;
      }
    };

    // in the simulation. Use this function to induce minor disruptions.

    function disturbVector(v) {
      if (v.x === 0) {
        v.x = 0.001;
      }

      if (v.y === 0) {
        v.y = 0.001;
      }

      if (v.z === 0) {
        v.z = 0.001;
      }

      return v;
    }

    function disturbScalar(n) {
      return n === 0 ? 0.001 : n;
    }
    /**
     * @class Wind
     *
     * @param {Object} [options]
     *   @param {THREE.Vector3} [options.direction]
     *   @param {number} [options.speed]
     *   @param {Function} [options.directionFn]
     *   @param {Function} [options.speedFn]
     */


    var Wind =
    /*#__PURE__*/
    function () {
      function Wind(options) {
        _classCallCheck(this, Wind);

        var settings = Object.assign({}, this.constructor.defaults, options);
        this.direction = settings.direction;
        this.speed = settings.speed;
        this.directionFn = settings.directionFn;
        this.speedFn = settings.speedFn;
        this.force = new THREE.Vector3();
      }

      _createClass(Wind, [{
        key: "update",
        value: function update() {
          var time = Date.now();
          this.directionFn(disturbVector(this.force.copy(this.direction)), time).normalize().multiplyScalar(this.speedFn(disturbScalar(this.speed), time));
        }
      }]);

      return Wind;
    }();

    Wind.defaults = {
      direction: new THREE.Vector3(1, 0, 0),
      speed: 100,
      directionFn: WindModifiers.blowFromLeftDirection,
      speedFn: WindModifiers.constantSpeed
    };

    var loader = new THREE.ImageLoader();
    loader.setCrossOrigin('anonymous');
    /**
     * @function loadImage
     *
     * @description Helper for loading CORS enabled images.
     *
     * @param {string} src
     * @param {Function} [callback]
     * @param {Function} [error]
     */

    function loadImage(src, callback, error) {
      var url = window.imageLocation + '/' + src + '.png';
      loader.load(url, callback, null, function (e) {
        console.error("FlagWaver.loadImage: Failed to load image from ".concat(src, "."));

        if (error) {
          error(e);
        }
      });
    }

    function getAngleOfSide(side) {
      switch (side) {
        case Side.TOP:
          return 0;

        case Side.LEFT:
          return -Math.PI / 2;

        case Side.BOTTOM:
          return Math.PI;

        case Side.RIGHT:
          return Math.PI / 2;

        default:
          return 0;
      }
    }

    var defaults = {
      width: 'auto',
      height: 'auto',
      hoisting: Hoisting.DEXTER,
      orientation: Side.TOP
    }; // Calculate width and/or height from image if either is set to 'auto'

    function computeSizeFromImage(image, options) {
      if (options.width === 'auto' && options.height === 'auto') {
        // Distance between flag poles is innerWidth * 0.24
        var crossWidth = window.innerWidth * 0.21;

        if (image.width < image.height) {
          // Vertical
          return {
            width: crossWidth,
            height: crossWidth * image.width / image.height
          };
        } else {
          // Horizontal or square
          return {
            width: crossWidth,
            height: crossWidth * image.height / image.width
          };
        }
      } else if (options.width === 'auto' && isNumeric(options.height)) {
        return {
          width: options.height * image.width / image.height,
          height: options.height
        };
      } else if (isNumeric(options.width) && options.height === 'auto') {
        return {
          width: options.width,
          height: options.width * image.height / image.width
        };
      } else {
        return {
          width: options.width,
          height: options.height
        };
      }
    } // Compute a numeric width and height from options


    function computeSize(image, options) {
      var size = {
        width: options.width,
        height: options.height
      };

      if (image) {
        size = computeSizeFromImage(image, size);
      }

      if (isNumeric(size.width) && isNumeric(size.height)) {
        return size;
      } else {
        return {
          width: Flag.defaults.width,
          height: Flag.defaults.height
        };
      }
    } // Check if flag has been rotated into a vertical position


    function isVertical(options) {
      return options.orientation === Side.LEFT || options.orientation === Side.RIGHT;
    } // Compute values needed to apply texture onto mesh


    function computeTextureArgs(options) {
      var result = {};
      result.reflect = options.hoisting === Hoisting.SINISTER;
      result.rotate = getAngleOfSide(options.orientation);
      return result;
    } // Generate transformed texture from image


    function createTextureFromImage(image, options) {
      var texture = new THREE.Texture(image);
      texture.matrixAutoUpdate = false;

      if (isObject(options)) {
        var matrix = texture.matrix;
        matrix.scale(1, 1); // Reflect

        if (options.reflect) {
          matrix.translate(-1, 0).scale(-1, 1);
        } // Rotate around center


        if (isNumeric(options.rotate)) {
          matrix.translate(-0.5, -0.5).rotate(-options.rotate).translate(0.5, 0.5);
        }
      }

      return texture;
    } // Compute values needed to create new flag


    function computeFlagArgs(image, options) {
      var result = Object.assign({}, options);

      if (isVertical(options)) {
        result.width = options.height;
        result.height = options.width;
      }

      if (image) {
        result.texture = createTextureFromImage(image, computeTextureArgs(options));
      }

      return result;
    }
    /**
     * @function buildRectangularFlagFromImage
     *
     * @description Helper for generating flags from rectangular designs
     * that can be rotated and flipped.
     *
     * @param {HTMLImageElement} image
     * @param {Object} [options]
     */


    function buildRectangularFlagFromImage(image, options) {
      var settings = Object.assign({}, defaults, options);
      Object.assign(settings, computeSize(image, settings)); // Init models and create meshes once images(s) have loaded

      return new Flag(computeFlagArgs(image, settings));
    }

    function ensureNumericSize(options) {
      var result = Object.assign({}, options);

      if (!isNumeric(result.width)) {
        result.width = Flag.defaults.width;
      }

      if (!isNumeric(result.height)) {
        result.height = Flag.defaults.height;
      }

      return result;
    }
    /**
     * @function buildFlag
     *
     * @description Helper for generating flags based on provided image
     * and options.
     *
     * @param {HTMLImageElement} image
     * @param {Object} [options]
     */


    function buildFlag(image, options) {
      if (image) {
        return buildRectangularFlagFromImage(image, options);
      }

      return new Flag(ensureNumericSize(options));
    }

    /**
     * @class FlagInterface
     *
     * @classdesc A wrapper object for managing a single flag.
     *
     * @param {Object} [options] - Options passed to buildFlag
     *   @param {string} [options.imgSrc] - Image to generate flag from
     */

    var FlagInterface =
    /*#__PURE__*/
    function () {
      function FlagInterface(options) {
        var _this = this;

        _classCallCheck(this, FlagInterface);

        this.flag = buildFlag(null, options);
        this.object = new THREE.Object3D();
        this.object.add(this.flag.object);
        loadImage(options.imgSrc, function (image) {
          _this.destroy();

          _this.flag = buildFlag(image, options);

          _this.object.add(_this.flag.object);
        });
      }

      _createClass(FlagInterface, [{
        key: "destroy",
        value: function destroy() {
          if (this.flag) {
            this.object.remove(this.flag.object);
            this.flag.destroy();
          }
        }
      }, {
        key: "reset",
        value: function reset() {
          this.flag.reset();
        }
      }, {
        key: "simulate",
        value: function simulate(deltaTime) {
          this.flag.simulate(deltaTime);
        }
      }, {
        key: "render",
        value: function render() {
          this.flag.render();
        }
      }]);

      return FlagInterface;
    }();

    /**
     * @function createPoleGeometryTypeI
     *
     * @description Build a standard flagpole.
     *
     *
     *   o
     *   |^^^^^
     *   |    ^
     *   |^^^^^
     *   |
     *   |
     *
     *
     * @param {Object} options
     */

    function createPoleGeometryTypeI(options) {
      var geometry = new THREE.CylinderGeometry(options.poleWidth, options.poleWidth, options.poleLength); // Center

      geometry.translate(0, -options.poleLength / 2, 0); // Add finial cap

      geometry.merge(new THREE.CylinderGeometry(options.poleCapSize, options.poleCapSize, options.poleCapSize));
      return geometry;
    }

    /**
     * @class Flagpole
     *
     * @classdesc Creates a geometry, material, and mesh for a flagpole.
     *
     * @param {Object} [options]
     *   @param {FlagpoleType} [options.flagpoleType]
     *   @param {number} [options.poleWidth]
     *   @param {number} [options.poleLength]
     *   @param {number} [options.poleCapSize]
     *   @param {number} [options.crossbarWidth]
     *   @param {number} [options.crossbarLength]
     *   @param {number} [options.crossbarCapSize]
     *   @param {number} [options.poleTopOffset]
     */

    var Flagpole =
    /*#__PURE__*/
    function () {
      function Flagpole(options) {
        _classCallCheck(this, Flagpole);

        var settings = Object.assign({}, this.constructor.defaults, options); // Geometry

        var geometry = this.buildGeometry(settings); // Material

        var material = new THREE.MeshPhongMaterial({
          color: 0x6A6A6A,
          specular: 0xffffff,
          shininess: 18
        }); // Mesh

        var mesh = new THREE.Mesh(geometry, material);
        mesh.receiveShadow = true;
        mesh.castShadow = true;
        this.mesh = mesh;
        this.object = this.mesh;
      }

      _createClass(Flagpole, [{
        key: "destroy",
        value: function destroy() {
          if (this.mesh instanceof THREE.Mesh) {
            this.mesh.material.dispose();
            this.mesh.geometry.dispose();
          }
        }
      }, {
        key: "buildGeometry",
        value: function buildGeometry(options) {
          return createPoleGeometryTypeI(options);
        }
      }, {
        key: "addFlag",
        value: function addFlag(flag) {
          flag.unpin();
          flag.pin({
            edges: [Side.LEFT]
          });
          flag.setLengthConstraints(Side.LEFT);
        }
      }]);

      return Flagpole;
    }();

    Flagpole.defaults = function () {
      var o = {};
      o.flagpoleType = FlagpoleType.VERTICAL;
      o.poleWidth = 6;
      o.poleLength = 8192;
      o.poleCapSize = o.poleWidth + 2;
      o.crossbarWidth = o.poleWidth - 2;
      o.crossbarLength = 200;
      o.crossbarCapSize = o.crossbarWidth + 2;
      o.poleTopOffset = 60;
      return o;
    }();

    /**
     * @function buildFlagpole
     *
     * @description Helper for generating different types of flagpoles.
     *
     * @param {Object} [options]
     */

    function buildFlagpole(options) {
      var settings = Object.assign({}, options);
      var flagpole;
      flagpole = new Flagpole(settings);
      return flagpole;
    }

    /**
     * @class FlagGroupInterface
     *
     * @classdesc A wrapper object for managing a flagpole and flag.
     *
     * @param {Object} [options] - Options passed to buildFlag and buildFlagpole
     */

    var FlagGroupInterface =
    /*#__PURE__*/
    function () {
      function FlagGroupInterface(options) {
        _classCallCheck(this, FlagGroupInterface);

        // ChT: the duration to raise the 1st place flag
        this.duration = options.duration;
        this.object = new THREE.Object3D();
        this.flagpole = buildFlagpole({});
        this.flagInterfaces = [];
        var _iteratorNormalCompletion = true;
        var _didIteratorError = false;
        var _iteratorError = undefined;

        try {
          for (var _iterator = options.imgSrc.split(',')[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
            var src = _step.value;
            this.flagInterfaces.push(new FlagInterface({
              imgSrc: src
            }));
          }
        } catch (err) {
          _didIteratorError = true;
          _iteratorError = err;
        } finally {
          try {
            if (!_iteratorNormalCompletion && _iterator.return != null) {
              _iterator.return();
            }
          } finally {
            if (_didIteratorError) {
              throw _iteratorError;
            }
          }
        }

        this.object.add(this.flagpole.object);
        var _iteratorNormalCompletion2 = true;
        var _didIteratorError2 = false;
        var _iteratorError2 = undefined;

        try {
          for (var _iterator2 = this.flagInterfaces[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
            var flagInterface = _step2.value;
            this.object.add(flagInterface.object);
            this.flagpole.addFlag(flagInterface.flag);
          }
          /*
                  this.setFlagpoleOptions(options);
                  this.setFlagOptions(options);
           */
          // ChT: We need our own clock to raise the flag in real time,
          // even on low performance devices

        } catch (err) {
          _didIteratorError2 = true;
          _iteratorError2 = err;
        } finally {
          try {
            if (!_iteratorNormalCompletion2 && _iterator2.return != null) {
              _iterator2.return();
            }
          } finally {
            if (_didIteratorError2) {
              throw _iteratorError2;
            }
          }
        }

        this.clock = new THREE.Clock();
      }

      _createClass(FlagGroupInterface, [{
        key: "destroy",
        value: function destroy() {
          if (this.flagpole) {
            this.object.remove(this.flagpole.object);
            this.flagpole.destroy();
          }

          var _iteratorNormalCompletion3 = true;
          var _didIteratorError3 = false;
          var _iteratorError3 = undefined;

          try {
            for (var _iterator3 = this.flagInterfaces[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {
              var flagInterface = _step3.value;
              this.object.remove(flagInterface.object);
              flagInterface.destroy();
            }
          } catch (err) {
            _didIteratorError3 = true;
            _iteratorError3 = err;
          } finally {
            try {
              if (!_iteratorNormalCompletion3 && _iterator3.return != null) {
                _iterator3.return();
              }
            } finally {
              if (_didIteratorError3) {
                throw _iteratorError3;
              }
            }
          }

          this.flagInterfaces = [];
        }
      }, {
        key: "reset",
        value: function reset() {}
      }, {
        key: "simulate",
        value: function simulate(deltaTime) {
          // ChT: Raise the flag! 
          if (!window.FW_App.raiseFlags) return; // Get our own delta because the argument  may be shorter than the 
          // real time difference

          var delta = this.clock.getDelta(); // Calculate offset

          var flagStart = window.innerWidth * 0.21 * 2 / 3 * 1.1 * 2.2;
          var offset = (window.innerHeight * 0.9 - flagStart) / this.duration * delta;
          var maxY = this.flagpole.object.position.y - 4;

          if (this.flagpole && this.flagInterfaces) {
            var _iteratorNormalCompletion4 = true;
            var _didIteratorError4 = false;
            var _iteratorError4 = undefined;

            try {
              for (var _iterator4 = this.flagInterfaces[Symbol.iterator](), _step4; !(_iteratorNormalCompletion4 = (_step4 = _iterator4.next()).done); _iteratorNormalCompletion4 = true) {
                var flagInterface = _step4.value;
                if (maxY > flagInterface.object.position.y + offset) flagInterface.object.position.y += offset;else if (maxY > flagInterface.object.y) flagInterface.object.position.y = maxY;
                maxY -= flagInterface.flag.cloth.height * 1.1;
              }
            } catch (err) {
              _didIteratorError4 = true;
              _iteratorError4 = err;
            } finally {
              try {
                if (!_iteratorNormalCompletion4 && _iterator4.return != null) {
                  _iterator4.return();
                }
              } finally {
                if (_didIteratorError4) {
                  throw _iteratorError4;
                }
              }
            }
          }
        }
      }, {
        key: "render",
        value: function render() {}
      }]);

      return FlagGroupInterface;
    }();

    FlagGroupInterface.FlagInterface = FlagInterface;

    var tmp = new THREE.Vector3();
    var worldPosition = new THREE.Vector3();
    /**
     * @function localizeForce
     *
     * @description Transforms a force vector from global space to local space.
     *
     * @param {THREE.Vector3} force - Vector representing a force
     * @param {THREE.Object3D} [object] - Local object
     */

    function localizeForce(force, object) {
      /*
       * Converts the direction and magnitude of a given vector from
       * world coordinate space to the local space of the given object.
       * The given vector is expected to represent direction and magnitude
       * only, it does not represent a position in 3D space.
       */
      tmp.copy(force);

      if (object instanceof THREE.Object3D) {
        // Discard world position information
        worldPosition.setFromMatrixPosition(object.matrixWorld);
        tmp.add(worldPosition);
        object.worldToLocal(tmp);
      }

      return tmp;
    }

    // We make the stuff lighter, flying more horizontal

    var gravity = new THREE.Vector3(0, -G * 14, 0);
    /**
     * @function applyGravityToCloth
     *
     * @description Applies downward gravity force to cloth.
     *
     * @param {Cloth} cloth
     * @param {THREE.Object3D} [object]
     */

    function applyGravityToCloth(cloth, object) {
      var particles = cloth.particles;
      var force = localizeForce(gravity, object);

      for (var i = 0, ii = particles.length; i < ii; i++) {
        particles[i].acceleration.add(force);
      }
    }

    var tmp$1 = new THREE.Vector3();

    var tmp$2 = new THREE.Vector3();
    /**
     * @function applyWindForceToCloth
     *
     * @description Applies wind force to cloth.
     *
     * @param {Cloth} cloth
     * @param {Wind} wind
     * @param {THREE.Object3D} [object]
     */

    function applyWindForceToCloth(cloth, wind, object) {
      var particles = cloth.particles;
      var faces = cloth.geometry.faces;

      if (wind) {
        var force = localizeForce(wind.force, object);

        for (var i = 0, ii = faces.length; i < ii; i++) {
          var face = faces[i];
          var normal = face.normal;
          tmp$2.copy(normal).normalize().multiplyScalar(normal.dot(force));
          particles[face.a].applyForce(tmp$2);
          particles[face.b].applyForce(tmp$2);
          particles[face.c].applyForce(tmp$2);
        }
      }
    }

    /**
     * @class Module
     * @interface
     *
     * @classdesc A Module encapsulates a piece of functionality that can
     * be applied to a scene. This class is just a skeleton for other classes
     * to inherit from.
     *
     * Each module should have an `init` method and a `deinit` method which
     * should be called whenever it is added to or removed from a scene.
     */
    var Module =
    /*#__PURE__*/
    function () {
      function Module() {
        _classCallCheck(this, Module);
      }

      _createClass(Module, [{
        key: "init",
        value: function init() {}
      }, {
        key: "deinit",
        value: function deinit() {}
      }]);

      return Module;
    }();

    Module.displayName = 'module';

    /**
     * @class ModuleSystem
     *
     * @classdesc Manages a collection of modules.
     *
     * @param {App} [context]
     */

    var ModuleSystem =
    /*#__PURE__*/
    function () {
      function ModuleSystem(context) {
        _classCallCheck(this, ModuleSystem);

        this.context = context || this;
        this.modules = [];
      } // Get module by display name and index


      _createClass(ModuleSystem, [{
        key: "module",
        value: function module(displayName) {
          var index = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 0;
          var modules = this.modules;

          for (var i = 0, ii = modules.length, j = 0; i < ii; i++) {
            var module = modules[i];

            if (module.constructor.displayName === displayName) {
              if (j === index) {
                return module;
              }

              j++;
            }
          }

          return null;
        } // Add module and call `module.init`

      }, {
        key: "add",
        value: function add(module) {
          if (!(module instanceof Module)) {
            return;
          }

          if (module.init) {
            module.init(this.context);
          }

          this.modules.push(module);
          return module;
        } // Remove module and call `module.deinit`

      }, {
        key: "remove",
        value: function remove(module) {
          if (!(module instanceof Module)) {
            return;
          }

          var modules = this.modules;
          var index = modules.indexOf(module);

          if (index < 0) {
            return;
          }

          if (module.deinit) {
            module.deinit(this.context);
          }

          return modules.splice(index, 1)[0];
        }
      }]);

      return ModuleSystem;
    }();

    /**
     * @class App
     *
     * @classdesc Root module and time counter.
     *
     * @param {Object} options
     *   @param {THREE.Scene} options.scene
     *   @param {THREE.Camera} options.camera
     *   @param {THREE.WebGLRenderer} options.renderer
     *   @param {Function} [options.onStart]
     *   @param {Function} [options.onUpdate]
     */

    var App =
    /*#__PURE__*/
    function (_ModuleSystem) {
      _inherits(App, _ModuleSystem);

      function App(options) {
        var _this;

        _classCallCheck(this, App);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(App).call(this));
        var settings = Object.assign({}, _this.constructor.defaults, options);
        var scene = settings.scene,
            camera = settings.camera,
            renderer = settings.renderer;
        var onStart = settings.onStart.bind(_assertThisInitialized(_this));
        var onUpdate = settings.onUpdate.bind(_assertThisInitialized(_this));
        var clock = new THREE.Clock();
        var timestep = TIME_STEP;

        var startModules = function startModules() {
          var modules = _this.modules;

          for (var i = 0, ii = modules.length; i < ii; i++) {
            var module = modules[i];

            if (module.subject && module.reset) {
              module.reset();
            }
          }
        };

        var updateModules = function updateModules(deltaTime) {
          var modules = _this.modules;

          for (var i = 0, ii = modules.length; i < ii; i++) {
            var module = modules[i];

            if ((module.subject || module.interact) && module.update) {
              module.update(deltaTime);
            }
          }
        };

        var render = function render() {
          camera.lookAt(scene.position);
          renderer.render(scene, camera);
        };

        var start = function start() {
          onStart();
          startModules();
          render();
        };

        var update = function update(deltaTime) {
          onUpdate(deltaTime);
          updateModules(deltaTime);
          render();
        };

        var loop = function loop() {
          requestAnimationFrame(loop);

          if (clock.running) {
            // ChT: Animation in slow motion. Three would still call us
            // with 60fps, but we fake half speed
            update(Math.min(clock.getDelta() / 2, timestep));
          }
        }; // Init


        scene.add(camera);
        clock.start();
        start();
        loop(); // Public properties and methods

        _this.scene = scene;
        _this.renderer = renderer;
        _this.camera = camera;
        _this.canvas = renderer.domElement;
        _this.clock = clock;
        _this.timestep = timestep;
        _this.start = start;
        _this.update = update;
        return _this;
      }

      return App;
    }(ModuleSystem);

    App.defaults = {
      onStart: function onStart() {},
      onUpdate: function onUpdate() {}
    };

    /**
     * @class AnimationModule
     *
     * @classdesc Allow animation to be paused and continued.
     */

    var AnimationModule =
    /*#__PURE__*/
    function (_Module) {
      _inherits(AnimationModule, _Module);

      function AnimationModule() {
        var _this;

        _classCallCheck(this, AnimationModule);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(AnimationModule).call(this));
        _this.app = null;
        return _this;
      }

      _createClass(AnimationModule, [{
        key: "init",
        value: function init(app) {
          this.app = app;
        }
      }, {
        key: "deinit",
        value: function deinit() {
          if (this.play) {
            this.play();
          }
        }
      }, {
        key: "play",
        value: function play() {
          var clock = this.app.clock;

          if (!clock.running) {
            clock.start();
          }
        }
      }, {
        key: "pause",
        value: function pause() {
          this.app.clock.stop();
        }
      }, {
        key: "step",
        value: function step() {
          var _this$app = this.app,
              clock = _this$app.clock,
              timestep = _this$app.timestep;

          if (!clock.running) {
            clock.elapsedTime += timestep;
            this.app.update(timestep);
          }
        }
      }, {
        key: "reset",
        value: function reset() {
          var clock = this.app.clock;
          clock.startTime = 0;
          clock.oldTime = 0;
          clock.elapsedTime = 0;
          this.app.start();
        }
      }]);

      return AnimationModule;
    }(Module);

    AnimationModule.displayName = 'animationModule';

    /**
     * @class ResizeModule
     *
     * @classdesc Updates canvas size on window resize.
     */

    var ResizeModule =
    /*#__PURE__*/
    function (_Module) {
      _inherits(ResizeModule, _Module);

      function ResizeModule() {
        var _this;

        _classCallCheck(this, ResizeModule);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(ResizeModule).call(this));
        _this.app = null;
        _this.resize = _this.resize.bind(_assertThisInitialized(_this));
        return _this;
      }

      _createClass(ResizeModule, [{
        key: "init",
        value: function init(app) {
          this.app = app;
          window.addEventListener('resize', this.resize);
          this.resize();
        }
      }, {
        key: "deinit",
        value: function deinit() {
          window.removeEventListener('resize', this.resize);
        }
      }, {
        key: "resize",
        value: function resize() {
          var _this$app = this.app,
              scene = _this$app.scene,
              camera = _this$app.camera,
              renderer = _this$app.renderer;
          var parentElement = renderer.domElement.parentElement;
          var canvasHeight = 1;
          var canvasWidth = 1; // If canvas is added to DOM

          if (parentElement) {
            canvasWidth = parentElement.clientWidth;
            canvasHeight = parentElement.clientHeight;
          } // Update scene


          camera.aspect = canvasWidth / canvasHeight;
          camera.updateProjectionMatrix();
          renderer.setSize(canvasWidth, canvasHeight);
          renderer.render(scene, camera);
        }
      }]);

      return ResizeModule;
    }(Module);

    ResizeModule.displayName = 'resizeModule';

    var PropertyValidator =
    /*#__PURE__*/
    function () {
      function PropertyValidator(validators) {
        _classCallCheck(this, PropertyValidator);

        this.validators = validators || {};
      }

      _createClass(PropertyValidator, [{
        key: "validate",
        value: function validate(options, strict) {
          var validators = this.validators;

          if (isObject(options)) {
            return Object.keys(options).reduce(function (result, key) {
              var value = options[key];

              if (typeof value !== 'undefined') {
                if (validators[key]) {
                  var validated = validators[key](value);

                  if (validated != null) {
                    result[key] = validated;
                  }
                } else if (!strict) {
                  result[key] = value;
                }
              }

              return result;
            }, {});
          }

          return {};
        }
      }]);

      return PropertyValidator;
    }();

    function createPropertyValidator(validators) {
      var propertyValidator = new PropertyValidator(validators);
      return function (options, strict) {
        return propertyValidator.validate(options, strict);
      };
    }

    /**
     * @class ControlModule
     * @interface
     *
     * @classdesc A ControlModule is a wrapper that provides an interface
     * between the main app and a scene object.
     */

    var ControlModule =
    /*#__PURE__*/
    function (_Module) {
      _inherits(ControlModule, _Module);

      function ControlModule() {
        _classCallCheck(this, ControlModule);

        return _possibleConstructorReturn(this, _getPrototypeOf(ControlModule).apply(this, arguments));
      }

      return ControlModule;
    }(Module);

    ControlModule.displayName = 'controlModule';
    ControlModule.Subject = Object;

    /**
     * @class FlagModule
     *
     * @classdesc An interface for a single flag.
     *
     * @param {Object} [subject]
     * @param {THREE.Object3D} [context]
     */

    var FlagModule =
    /*#__PURE__*/
    function (_ControlModule) {
      _inherits(FlagModule, _ControlModule);

      function FlagModule(subject, context) {
        var _this;

        _classCallCheck(this, FlagModule);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(FlagModule).call(this));
        _this.subject = subject || null;
        _this.context = context || null;
        _this.configOptions = Object.assign({}, _this.constructor.Subject.defaults);
        return _this;
      }

      _createClass(FlagModule, [{
        key: "init",
        value: function init(app) {
          this.subject = this.subject || new this.constructor.Subject();

          if (!this.context) {
            app.scene.add(this.subject.object);
          }
        }
      }, {
        key: "deinit",
        value: function deinit(app) {
          if (!this.context) {
            app.scene.remove(this.subject.object);
            this.subject.destroy();
          }
        }
      }, {
        key: "reset",
        value: function reset() {
          this.subject.reset();
          this.subject.render();
        }
      }, {
        key: "update",
        value: function update(deltaTime) {
          this.subject.simulate(deltaTime);
          this.subject.render();
        }
      }, {
        key: "setOptions",
        value: function setOptions(options, callback) {
          var _this2 = this;

          if (isObject(options)) {
            this.subject.setOptions(Object.assign(this.configOptions, this.constructor.validate(options)), function (flag) {
              if (callback) {
                callback(_this2.configOptions);
              }
            });
          }
        }
      }]);

      return FlagModule;
    }(ControlModule);

    FlagModule.displayName = 'flagModule';
    FlagModule.Subject = FlagInterface;
    FlagModule.validate = createPropertyValidator({
      topEdge: function topEdge(value) {
        if (hasValue(Side, value)) {
          return value;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      hoisting: function hoisting(value) {
        if (hasValue(Hoisting, value)) {
          return value;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      width: function width(value) {
        var n = Number(value);

        if (isNumeric(value) && n > 0) {
          return n;
        } else if (value === 'auto') {
          return value;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      height: function height(value) {
        var n = Number(value);

        if (isNumeric(value) && n > 0) {
          return n;
        } else if (value === 'auto') {
          return value;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      mass: function mass(value) {
        var n = Number(value);

        if (isNumeric(value) && n >= 0) {
          return n;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      granularity: function granularity(value) {
        var n = Math.round(value);

        if (isNumeric(value) && n > 0) {
          return n;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      imgSrc: function imgSrc(value) {
        if (typeof value === 'string') {
          return value;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      },
      flagpoleType: function flagpoleType(value) {
        if (hasValue(FlagpoleType, value)) {
          return value;
        } else {
          console.error('FlagWaver.FlagModule.option: Invalid value.');
        }
      }
    });

    /**
     * @class FlagGroupModule
     *
     * @classdesc An interface for a flagpole and its flag.
     *
     * @param {Object} [subject]
     * @param {THREE.Object3D} [context]
     */

    var FlagGroupModule =
    /*#__PURE__*/
    function (_ControlModule) {
      _inherits(FlagGroupModule, _ControlModule);

      function FlagGroupModule(flagOpt) {
        var _this;

        _classCallCheck(this, FlagGroupModule);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(FlagGroupModule).call(this));
        _this.subject = null;
        _this.context = null;
        _this.flags = [];
        _this.flagOpts = flagOpt;
        return _this;
      }

      _createClass(FlagGroupModule, [{
        key: "init",
        value: function init(app) {
          this.subject = new this.constructor.Subject(this.flagOpts);

          if (!this.context) {
            app.scene.add(this.subject.object);
          }

          var _iteratorNormalCompletion = true;
          var _didIteratorError = false;
          var _iteratorError = undefined;

          try {
            for (var _iterator = this.subject.flagInterfaces[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
              var flagInterface = _step.value;
              this.flags.push(new FlagModule(flagInterface, this.subject.object));
            }
          } catch (err) {
            _didIteratorError = true;
            _iteratorError = err;
          } finally {
            try {
              if (!_iteratorNormalCompletion && _iterator.return != null) {
                _iterator.return();
              }
            } finally {
              if (_didIteratorError) {
                throw _iteratorError;
              }
            }
          }

          var _iteratorNormalCompletion2 = true;
          var _didIteratorError2 = false;
          var _iteratorError2 = undefined;

          try {
            for (var _iterator2 = this.flags[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
              var _flag = _step2.value;
              app.add(_flag);
            }
          } catch (err) {
            _didIteratorError2 = true;
            _iteratorError2 = err;
          } finally {
            try {
              if (!_iteratorNormalCompletion2 && _iterator2.return != null) {
                _iterator2.return();
              }
            } finally {
              if (_didIteratorError2) {
                throw _iteratorError2;
              }
            }
          }
        }
      }, {
        key: "deinit",
        value: function deinit(app) {
          var _iteratorNormalCompletion3 = true;
          var _didIteratorError3 = false;
          var _iteratorError3 = undefined;

          try {
            for (var _iterator3 = this.flags[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {
              flag = _step3.value;
              app.remove(flag);
            }
          } catch (err) {
            _didIteratorError3 = true;
            _iteratorError3 = err;
          } finally {
            try {
              if (!_iteratorNormalCompletion3 && _iterator3.return != null) {
                _iterator3.return();
              }
            } finally {
              if (_didIteratorError3) {
                throw _iteratorError3;
              }
            }
          }

          if (!this.context) {
            app.scene.remove(this.subject.object);
            this.subject.destroy();
          }
        }
      }, {
        key: "reset",
        value: function reset() {
          this.subject.reset();
          this.subject.render();
        }
      }, {
        key: "update",
        value: function update(deltaTime) {
          this.subject.simulate(deltaTime);
          this.subject.render();
        }
      }, {
        key: "moveFlags",
        value: function moveFlags(y, offset) {
          var ypos = y;
          var _iteratorNormalCompletion4 = true;
          var _didIteratorError4 = false;
          var _iteratorError4 = undefined;

          try {
            for (var _iterator4 = this.subject.flagInterfaces[Symbol.iterator](), _step4; !(_iteratorNormalCompletion4 = (_step4 = _iterator4.next()).done); _iteratorNormalCompletion4 = true) {
              var flagInterface = _step4.value;
              flagInterface.object.position.y = ypos;
              ypos -= offset;
            }
          } catch (err) {
            _didIteratorError4 = true;
            _iteratorError4 = err;
          } finally {
            try {
              if (!_iteratorNormalCompletion4 && _iterator4.return != null) {
                _iterator4.return();
              }
            } finally {
              if (_didIteratorError4) {
                throw _iteratorError4;
              }
            }
          }
        }
      }]);

      return FlagGroupModule;
    }(ControlModule);

    FlagGroupModule.displayName = 'flagGroupModule';
    FlagGroupModule.Subject = FlagGroupInterface;

    /**
     * @class WindModule
     *
     * @classdesc Adds wind to scene.
     *
     * @param {Wind} wind
     */


    var WindModule =
    /*#__PURE__*/
    function (_ControlModule) {
      _inherits(WindModule, _ControlModule);

      function WindModule(options) {
        var _this;

        _classCallCheck(this, WindModule);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(WindModule).call(this));
        _this.subject = new _this.constructor.Subject();
        _this.configOptions = Object.assign({}, _this.constructor.Subject.defaults);
        if (options !== undefined) _this.setOptions(options);
        return _this;
      }

      _createClass(WindModule, [{
        key: "update",
        value: function update(deltaTime) {
          this.subject.update(deltaTime);
        }
      }, {
        key: "setOptions",
        value: function setOptions(options) {
          this.subject = new this.constructor.Subject(Object.assign(this.configOptions, this.constructor.validate(options)));
        }
      }]);

      return WindModule;
    }(ControlModule);

    WindModule.displayName = 'windModule';
    WindModule.Subject = Wind;
    WindModule.validate = createPropertyValidator({
      speed: function speed(value) {
        var n = Number(value);

        if (isNumeric(value) && n >= 0) {
          return n;
        } else {
          console.error('FlagWaver.WindModule.option: Invalid value.');
        }
      }
    });

    /**
     * @class InteractionModule
     * @interface
     *
     * @classdesc A module that mediates physical interactions.
     *
     * @param {string[]} [subjectTypes] - Modules of subjects that are acted upon.
     * @param {string[]} [actionTypes] - Modules that cause an action on subjects.
     */

    var InteractionModule =
    /*#__PURE__*/
    function (_Module) {
      _inherits(InteractionModule, _Module);

      function InteractionModule(subjectTypes, actionTypes) {
        var _this;

        _classCallCheck(this, InteractionModule);

        _this = _possibleConstructorReturn(this, _getPrototypeOf(InteractionModule).call(this));
        _this.app = null;
        _this.subjectTypes = subjectTypes || [];
        _this.actionTypes = actionTypes || [];
        _this.subjects = [];
        _this.actions = [];
        _this.needsUpdate = false;
        return _this;
      }

      _createClass(InteractionModule, [{
        key: "updateModulesList",
        value: function updateModulesList() {
          var app = this.app;

          if (!app) {
            return;
          }

          var modules = app.modules;
          var subjectTypes = this.subjectTypes;
          var actionTypes = this.actionTypes;
          var subjects = [];
          var actions = [];

          for (var i = 0, ii = modules.length; i < ii; i++) {
            var module = modules[i];

            if (subjectTypes.indexOf(module.constructor.displayName) >= 0) {
              subjects.push(module.subject);
            }

            if (actionTypes.indexOf(module.constructor.displayName) >= 0) {
              actions.push(module.subject);
            }
          }

          this.subjects = subjects;
          this.actions = actions;
        }
      }, {
        key: "init",
        value: function init(app) {
          this.app = app;
          this.updateModulesList();
        }
      }, {
        key: "interact",
        value: function interact(subject, action) {}
      }, {
        key: "update",
        value: function update(deltaTime) {
          var interact = this.interact;

          if (this.needsUpdate) {
            this.updateModulesList();
            this.needsUpdate = false;
          }

          var subjects = this.subjects;
          var actions = this.actions;

          if (actions.length) {
            for (var i = 0, ii = actions.length; i < ii; i++) {
              for (var j = 0, jl = subjects.length; j < jl; j++) {
                interact(subjects[j], actions[i]);
              }
            }
          } else {
            for (var _j = 0, _jl = subjects.length; _j < _jl; _j++) {
              interact(subjects[_j]);
            }
          }
        }
      }]);

      return InteractionModule;
    }(Module);

    InteractionModule.displayName = 'interactionModule';

    /**
     * @class GravityModule
     *
     * @classdesc Applies gravity to subjects.
     */

    var GravityModule =
    /*#__PURE__*/
    function (_InteractionModule) {
      _inherits(GravityModule, _InteractionModule);

      function GravityModule() {
        _classCallCheck(this, GravityModule);

        return _possibleConstructorReturn(this, _getPrototypeOf(GravityModule).apply(this, arguments));
      }

      _createClass(GravityModule, [{
        key: "interact",
        value: function interact(subject) {
          applyGravityToCloth(subject.flag.cloth, subject.flag.object);
        }
      }]);

      return GravityModule;
    }(InteractionModule);

    GravityModule.displayName = 'gravityModule';

    /**
     * @class WindForceModule
     *
     * @classdesc Applies wind to subjects.
     */

    var WindForceModule =
    /*#__PURE__*/
    function (_InteractionModule) {
      _inherits(WindForceModule, _InteractionModule);

      function WindForceModule() {
        _classCallCheck(this, WindForceModule);

        return _possibleConstructorReturn(this, _getPrototypeOf(WindForceModule).apply(this, arguments));
      }

      _createClass(WindForceModule, [{
        key: "interact",
        value: function interact(subject, wind) {
          applyWindForceToCloth(subject.flag.cloth, wind, subject.flag.object);
        }
      }]);

      return WindForceModule;
    }(InteractionModule);

    WindForceModule.displayName = 'windForceModule';

    // Constants

    function parse(string) {
      var result = {};

      if (typeof string !== 'string') {
        return result;
      }

      var pairs = string.split('&');

      for (var i = 0, ii = pairs.length; i < ii; i++) {
        var pairString = pairs[i];

        if (pairString) {
          var pair = pairString.split('=');
          var key = pair[0];

          if (key) {
            // Set value to null if '=' sign is not present
            result[key] = typeof pair[1] !== 'undefined' ? pair.slice(1).join('=') : null;
          }
        }
      }

      return result;
    }
    function stringify(object) {
      var keys = Object.keys(object);
      var pairs = [];

      for (var i = 0, ii = keys.length; i < ii; i++) {
        var key = keys[i];
        var value = object[key];

        if (value === null) {
          pairs.push(key);
        } else if (typeof value !== 'undefined') {
          pairs.push(key + '=' + value);
        }
      }

      return pairs.join('&');
    }

    var fieldDefaults = {
      defaultValue: '',
      parse: function parse$$1(param) {
        return param;
      },
      stringify: function stringify$$1(value) {
        return value;
      }
    };

    function keysToLowerCase(object) {
      return Object.keys(object).reduce(function (result, key) {
        result[key.toLowerCase()] = object[key];
        return result;
      }, {});
    }

    function isIgnored(field, value) {
      return value === field.defaultValue || value === '' || value == null;
    }

    var ParamState =
    /*#__PURE__*/
    function () {
      function ParamState(fields) {
        _classCallCheck(this, ParamState);

        this.fields = Object.keys(fields).reduce(function (result, key) {
          result[key] = Object.assign({}, fieldDefaults, fields[key]);
          return result;
        }, {});
      }

      _createClass(ParamState, [{
        key: "parse",
        value: function parse$$1(string) {
          var fields = this.fields;
          var params = keysToLowerCase(parse(string));
          return Object.keys(fields).reduce(function (state, key) {
            var field = fields[key];
            var param = params[key];
            var value = param && field.parse(param);
            state[key] = !isIgnored(field, value) ? value : field.defaultValue;
            return state;
          }, {});
        }
      }, {
        key: "stringify",
        value: function stringify$$1(state) {
          var fields = this.fields;
          return stringify(Object.keys(fields).reduce(function (params, key) {
            var field = fields[key];
            var value = state[key];

            if (!isIgnored(field, value)) {
              params[key] = field.stringify(value);
            }

            return params;
          }, {}));
        }
      }]);

      return ParamState;
    }();

    var _window = window,
        history = _window.history,
        location = _window.location; // Check if browser supports the History API

    var isHistorySupported = !!(history && history.replaceState); // Check if browser allows history changes in current context
    // https://bugs.chromium.org/p/chromium/issues/detail?id=529313

    var isHistoryAllowed = function () {
      try {
        history.replaceState(null, '', location.href);
      } catch (e) {
        {
          console.log('Cannot push states to history object.');
          console.log(e.message);
        }

        return false;
      }

      return true;
    }();

    var initialized = false; // Functions
    //

    if (window.getParameterByName === undefined) {
      window.getParameterByName = function (name, def) {
        name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
        var regexS = "[\\?&]" + name + "=([^&#]*)";
        var regex = new RegExp(regexS);
        var results = regex.exec(window.location.search);

        if (results == null && parent != undefined) {

          results = regex.exec(parent.window.location.search);
        }

        if (results == null) return def;else return decodeURIComponent(results[1].replace(/\+/g, " "));
      };
    }

    function fromQuery() {
      var flags = [];

      for (var i = 0; i < 4; i++) {
        var imgURL = getParameterByName('' + (i + 1), null) || 'AUT';
        flags.push(imgURL);
      }

      return flags;
    }

    function buildScene() {
      var scene = new THREE.Scene();
      scene.fog = new THREE.Fog(0x000000, 1000, 10000);
      scene.fog.color.setHSL(0.6, 1, 0.9);
      return scene;
    }

    function buildCamera() {
      var defaultAngle = 30;
      var angle = parseInt(getParameterByName('cameraAngle', defaultAngle)); // ChT: Orthographic camera instead of perspective camera:
      // Basically we are rendering a flat 2D scene and equal distances are more
      // important than a 3D look
      // We still calculate the z distance based on the view angle

      /*    
        const camera = new THREE.PerspectiveCamera(
            angle,
            window.innerWidth / window.innerHeight,
            1,
            10000
        );
      */

      var camera = new THREE.OrthographicCamera(-window.innerWidth / 2, window.innerWidth / 2, window.innerHeight / 2, -window.innerHeight / 2, 1, 10000);
      camera.position.x = parseInt(getParameterByName('cameraX', '0'));
      camera.position.y = parseInt(getParameterByName('cameraY', '0'));
      camera.position.z = parseInt(getParameterByName('cameraZ', window.innerHeight / 2 / Math.tan(angle / 2 * Math.PI / 180)));
      return camera;
    }

    function buildRenderer() {
      var renderer = new THREE.WebGLRenderer({
        antialias: true,
        alpha: true
      });
      renderer.setSize(window.innerWidth, window.innerHeight);
      renderer.gammaInput = true;
      renderer.gammaOutput = true;
      renderer.shadowMap.enabled = true;
      return renderer;
    }

    function initLights(app) {
      var scene = app.scene;
      scene.add(new THREE.AmbientLight(0x222222));
      var light1 = new THREE.DirectionalLight(0xffffff, 1.75);
      var d = 300;
      light1.color.setHSL(0.6, 1, 0.9375);
      light1.position.set(50, 175, 100);
      light1.position.multiplyScalar(1.3);
      light1.castShadow = true;
      light1.shadowMapWidth = 2048;
      light1.shadowMapHeight = 2048;
      light1.shadowCameraTop = d;
      light1.shadowCameraLeft = -d;
      light1.shadowCameraBottom = -d;
      light1.shadowCameraRight = d;
      light1.shadowCameraFar = 1000;
      light1.shadowDarkness = 0.5;
      scene.add(light1);
      var light2 = new THREE.DirectionalLight(0xffffff, 0.35);
      light2.color.setHSL(0.3, 0.5, 0.75);
      light2.position.set(0, -1, 0);
      scene.add(light2);
    }

    function buildApp() {
      // Initialized location for flags from query parameter
      window.imageLocation = getParameterByName('flagLocation', window.imageLocation);
      var flags = fromQuery();
      var imgWidth = getParameterByName('flagWidth', 'auto');
      var imgHeight = getParameterByName('flagHeight', 'auto');
      var duration = parseInt(getParameterByName('duration', 30));
      var app = new App({
        scene: buildScene(),
        camera: buildCamera(),
        renderer: buildRenderer()
      });
      initLights(app);
      app.add(new ResizeModule());
      app.add(new AnimationModule());
      app.add(new WindModule({
        speed: getParameterByName('windSpeed', 100)
      })); // ChT: Add flagpoles for 2nd, 1st, and 3rd / 4th place

      app.add(new FlagGroupModule({
        imgSrc: flags[1],
        width: imgWidth,
        height: imgHeight,
        duration: duration
      }));
      app.add(new FlagGroupModule({
        imgSrc: flags[0],
        width: imgWidth,
        height: imgHeight,
        duration: duration
      }));
      app.add(new FlagGroupModule({
        imgSrc: flags[2],
        width: imgWidth,
        height: imgHeight,
        duration: duration
      }));
      app.add(new FlagGroupModule({
        imgSrc: flags[3],
        width: imgWidth,
        height: imgHeight,
        duration: duration
      })); // ChT: And put them in an orderly way, 2nd, 1st, 3rd and 4th
      // Distance between poles is 0.24 x innerWidth, flag width is 0.21 x innerWidth
      // Calculate offset of flag from bottom to give room for 2 flags
      // var flagStart = app.module(FlagModule.displayName, 0).subject.flag.cloth.height * 2.1;

      var flagDistance = window.innerWidth * 0.21 * 2 / 3 * 1.1;
      if (Number.isInteger(getParameterByName('flagHeight', 'auto'))) flagDistance = parseInt(getParameterByName('flagHeight', 'auto')) * 1.1;
      var flagStart = flagDistance * 2.2;
      app.module(FlagGroupModule.displayName, 0).subject.object.position.set(-(window.innerWidth * 0.45), window.innerHeight * 0.6 / 2, 0);
      app.module(FlagGroupModule.displayName, 0).moveFlags(-(window.innerHeight + window.innerHeight * 0.6) / 2 + flagStart, flagDistance);
      app.module(FlagGroupModule.displayName, 1).subject.object.position.set(-(window.innerWidth * 0.21), window.innerHeight * 0.8 / 2, 0);
      app.module(FlagGroupModule.displayName, 1).moveFlags(-(window.innerHeight + window.innerHeight * 0.8) / 2 + flagStart, flagDistance);
      app.module(FlagGroupModule.displayName, 2).subject.object.position.set(+(window.innerWidth * 0.03), window.innerHeight * 0.4 / 2, 0);
      app.module(FlagGroupModule.displayName, 2).moveFlags(-(window.innerHeight + window.innerHeight * 0.4) / 2 + flagStart, flagDistance);
      app.module(FlagGroupModule.displayName, 3).subject.object.position.set(+(window.innerWidth * 0.27), window.innerHeight * 0.4 / 2, 0);
      app.module(FlagGroupModule.displayName, 3).moveFlags(-(window.innerHeight + window.innerHeight * 0.4) / 2 + flagStart, flagDistance);
      app.add(new GravityModule(['flagModule']));
      app.add(new WindForceModule(['flagModule'], ['windModule']));
      return app;
    }

    function init() {
      // Prevent multiple initialization
      if (initialized) {
        throw new Error('Already initialized.');
      }

      var app = buildApp();
      initialized = true;
      var prestart = getParameterByName('prestart', 1);

      if (prestart > 0) {
        setTimeout(function () {
          // remove (in) visibility
          $('.bg-sky').css('visibility', '');
          app.raiseFlags = true;
        }, prestart * 1000);
      } else {
        // remove (in) visibility
        $('.bg-sky').css('visibility', '');
        app.raiseFlags = true;
      }

      return app;
    }

    /**
     * Flag Waver
     *
     * Simulate a flag waving in the breeze right in your browser window.
     *
     * /u/krikienoid
     *
     */
    var app; //
    // Flag Waver UI
    //

    (function (window, document, $) {
      //
      // Vars
      //
      //
      //
      // Init
      //
      $(document).ready(function () {
        //
        // Init
        //
        // Init flagWaver and append renderer to DOM
        app = init();
        window.FW_App = app;
        $('.js-flag-canvas').append(app.renderer.domElement);
        window.dispatchEvent(new window.Event('resize')); // On click anywhere in browser start raising the flags

        $('body').on('click', function () {
          FW_App.raiseFlags = true;
        });
      });
    })(window, document, jQuery);

    /*!
     * FlagWaver
     * @author krikienoid / https://github.com/krikienoid
     *
     * A web app for simulating a waving flag.
     */

}(THREE));

//# sourceMappingURL=app.js.map
