/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*global BigInteger, angular, _*/
(function () {
'use strict';

angular.module('widgets')
    .directive('widgetsStat', ['$timeout', 'socket', function ($timeout, socket) {
        return {
            restrict: 'A',
            templateUrl: 'views/stat.html',
            scope: {
                app: "=",
                label: "@",
                onClick: "&"
            },
            link: function($scope, iElement, iAttrs) {
                $scope.totalEmitted = 0;
                $scope.totalProcessed = 0;
                $scope.elapsed = 0;

                var initialElapsedTime;
                var startTime;

                function updatedElapsedTime() {
                    $scope.elapsed = initialElapsedTime + (Date.now() - startTime);
                    $timeout(updatedElapsedTime, 1000);
                }

                $scope.$watch('app', function (app) {
                    if (app) {
                        initialElapsedTime = parseInt(app.elapsedTime);
                        startTime = Date.now();
                        updatedElapsedTime();

                        var topic = 'applications.' + app.id;

                        socket.subscribe(topic, function (message) {
                            var appData = message.data;
                            $scope.totalEmitted = appData.tuplesEmittedPSMA;
                            $scope.totalProcessed = appData.totalTuplesProcessed;
                            $scope.$apply();
                        });
                    }
                });
            }
        };
    }]);

})();
