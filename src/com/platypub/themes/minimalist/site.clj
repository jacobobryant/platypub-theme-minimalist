(ns com.platypub.themes.minimalist.site
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [com.platypub.themes.common :as common]
            [hiccup.util :refer [raw-string]]))

(def footer-text
  [:div.sm:text-center.text-sm.leading-snug.w-full.px-3.pb-3.opacity-75
   "Made with "
   [:a.underline {:href "https://biffweb.com/p/announcing-platypub/"
                  :target "_blank"} "Platypub"]
   ". "
   (common/recaptcha-disclosure {:link-class "underline"})])

(defn parse-nav-links [{:keys [site]}]
  (->> (:nav-links site "")
       str/split-lines
       (remove empty?)
       (map #(str/split % #"\s+" 2))))

(defn join [sep xs]
  (rest (mapcat vector (repeat sep) xs)))

(defn navbar [{:keys [site] :as opts}]
  (let [nav-links (parse-nav-links opts)]
    [:div.pb-3.mb-5.border-b
     [:div
      [:a.hover:no-underline.text-2xl
       {:href "/"
        :style {:text-decoration "none"
                :color "#24242e"}}
       (:title site)]]
     [:div
      (join
       " · "
       (for [[href label] nav-links]
         [:a {:href href} label]))]]))

(def errors
  {"invalid-email" "It looks like that email is invalid. Try a different one."
   "recaptcha-failed" "reCAPTCHA check failed. Try again."
   "unknown" "There was an unexpected error. Try again."})

(defn subscribe-form [{:keys [account site]}]
  [:div.border-t.pt-3.pb-5
   [:div (:newsletter-description site)]
   [:div.h-2]
   [:script (raw-string "function onSubscribe(token) { document.getElementById('recaptcha-form').submit(); }")]
   [:form#recaptcha-form.mb-0
    {:action "/.netlify/functions/subscribe"
     :method "POST"}
    [:input {:type "hidden"
             :name "href"
             :_ "on load set my value to window.location.href"}]
    [:input {:type "hidden"
             :name "referrer"
             :_ "on load set my value to document.referrer"}]

    [:div.sm:flex.items-end.w-full
     [:input.w-full.flex-grow
      {:name "email"
       :type "email"
       :placeholder "Enter your email"
       :_ (str "on load "
               "make a URLSearchParams from window.location.search called p "
               "then set my value to p.get('email')")
       :class ["appearance-none"
               "border"
               "focus:outline-none"
               "px-2"
               "py-1"
               "ring-opacity-30"
               "rounded"
               "text-black"
               "w-full"]}]
     [:div.w-2.h-3.flex-shrink-0]
     [:div
      [:button.block.w-full.g-recaptcha
       {:type "submit"
        :data-sitekey (:recaptcha/site account)
        :data-callback "onSubscribe"
        :data-action "subscribe"
        :class ["bg-[#343a40]"
                "hover:bg-black"
                "px-3"
                "py-1"
                "disabled:opacity-50"
                "rounded"
                "text-center"
                "text-white"]}
       "Subscribe"]]]
    (for [[code explanation] errors]
      [:div.hidden.text-left.mt-1
       {:_ (str "on load if window.location.search.includes('error="
                code
                "') remove .hidden from me")}
       explanation])]])

(defn base-page [{:keys [site post page account] :as opts} & contents]
  (common/base-html
   (assoc opts :base/head (list
                           [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/4.0.0/github-markdown.min.css"}]
                           [:link {:rel "stylesheet" :href "/css/vs.css"}]
                           [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-core.min.js"
                                     :defer "defer"}]
                           [:script {:src (str "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/"
                                               "autoloader/prism-autoloader.min.js")
                                     :defer "defer"}]
                           [:script {:defer "defer"
                                     :src "data:application/javascript,eval(document.currentScript.textContent)"}
                            (raw-string "Prism.plugins.autoloader.languages_path = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/'; ")]))
   [:div.container.p-3.mx-auto.markdown-body.max-w-prose
    (navbar opts)
    contents
    (when-not (some #((:tags % #{}) "nosubscribe") [post page])
      (list
       [:div.h-8]
       (subscribe-form opts)))]))

(defn render-page [{:keys [page] :as opts}]
  (base-page opts (raw-string (:html page))))

(defn render-post [{:keys [post] :as opts}]
  (base-page
   opts
   [:div.text-3xl (:title post)]
   (when-some [date (common/format-date "d MMMM yyyy" (:published-at post))]
     (list
      [:div.h-1]
      [:div.text-sm date]))
   [:div.h-4]
   (raw-string (:html post))))

(def rfc3339 "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

(defn parse-date [date & [format]]
  (.parse (new java.text.SimpleDateFormat (or format rfc3339)) date))

(defn format-date [date & [format]]
  (.format (new java.text.SimpleDateFormat (or format rfc3339)) date))

(defn crop-date [d fmt]
  (-> d
      (format-date fmt)
      (parse-date fmt)))

(defn archive-page [{:keys [posts] :as opts}]
  (base-page
   (assoc opts :base/title "Archive")
   (for [[t posts] (->> posts
                        (remove #((:tags %) "unlisted"))
                        (group-by #(crop-date (:published-at %) "MMMM yyyy"))
                        (sort-by (comp - inst-ms first)))]
     (list
      [:div.text-sm.text-gray-500
       (format-date t "MMMM yyyy")]
      (for [post posts]
        [:div.mb-1
         [:a {:href (str "/p/" (:slug post) "/")}
          (str/lower-case (:title post))]])
      [:div.h-4]))))

(def pages
  {"/archive/" archive-page})

(defn assets!
  "Deprecated"
  []
  (->> (file-seq (io/file "assets"))
       (filter #(.isFile %))
       (run! #(io/copy % (doto (io/file "public" (subs (.getPath %) (count "assets/"))) io/make-parents)))))

(defn -main []
  (let [opts (-> (common/derive-opts (edn/read-string (slurp "input.edn")))
                 (update :posts (fn [posts]
                                  (remove #((:tags %) "hidden") posts))))
        sitemap-exclude (->> (:posts opts)
                             (filter #((:tags %) "unlisted"))
                             (map (fn [post]
                                    (re-pattern (str "/p/" (:slug post) "/")))))]
    (common/redirects! opts)
    (common/netlify-subscribe-fn! opts)
    (common/pages! opts render-page pages)
    (common/posts! opts render-post)
    (common/atom-feed! opts)
    (common/sitemap! {:exclude (concat [#"/subscribed/"]
                                       sitemap-exclude)})
    (assets!)
    (when (fs/exists? "main.css")
      (io/make-parents "public/css/_")
      (common/safe-copy "main.css" "public/css/main.css"))
    (fs/copy-tree (io/file (io/resource "com/platypub/themes/minimalist/public"))
                  "public"
                  {:replace-existing true}))
  nil)
