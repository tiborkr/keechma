(ns keechma.core
  (:require [reagent.core :as reagent :refer [atom]]
            [keechma.controller :as controller]
            [keechma.app-state :as app-state]
            [cljs.core.async :refer [<! >! chan close! put! alts! timeout]]
            [keechma.ui-component :as ui])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defonce running-app (clojure.core/atom nil))

(defrecord HelloWorld []
  controller/IController
  (params [_ route-params]
    (or (get-in route-params [:data :message]) "Hello!"))
  (start [_ params app-db]
    (assoc-in app-db [:kv :message] params))
  (handler [this app-db in-chan out-chan]
    (go 
      (loop [count 0]
        (let [[command args] (<! in-chan)]
          (when (= command :some-command)
            (reset! app-db (assoc-in @app-db [:kv :message] (str "Hello #" count))))
          (when command (recur (inc count))))))))

(defn message [app-db]
  (reaction
   (get-in @app-db [:kv :message])))

(defn main-renderer [ctx]
  [:h1
   "MESSAGE: "
   @(ui/subscription ctx :message)
   [:br]
   [:a {:href (ui/url ctx {:baz "qux"})} "Click Here"]
   [:br]
   [:a {:href (ui/url ctx {:message "Or Here"})} "Or here"]
   [:br]
   [:button {:on-click #(ui/send-command ctx :some-command)} "Send Command"]])

(def main-component (ui/constructor {:renderer main-renderer 
                                     :subscription-deps [:message]}))

(def app-definition {:html-element (.getElementById js/document "app")
                     :controllers {:hello (->HelloWorld)}
                     :components {:main (-> main-component
                                            (assoc :topic :hello)
                                            (ui/resolve-subscription-dep :message message))}})

(defn start-app! []
  (reset! running-app (app-state/start! app-definition)))

(defn restart-app! []
  (let [current @running-app]
    (if current
      (app-state/stop! current start-app!)
      (start-app!))))
 
(restart-app!)

(defn on-js-reload []
  (println "CALLING ON JS RELOAD")
  ;;(restart-app!)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)